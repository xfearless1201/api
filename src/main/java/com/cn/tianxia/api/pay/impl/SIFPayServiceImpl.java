package com.cn.tianxia.api.pay.impl;

import com.cn.tianxia.api.common.PayEntity;
import org.apache.commons.codec.digest.DigestUtils;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.pay.MapUtils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.vo.ProcessNotifyVO;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Vicky
 * @version 1.2.0
 * @ClassName SIFPayServiceImpl
 * @Description 四方支付  渠道 支付宝、微信PC H5
 * @Date 2019/4/24 18 52
 **/
public class SIFPayServiceImpl extends PayAbstractBaseService implements PayService {

    private static final Logger logger = LoggerFactory.getLogger(SIFPayServiceImpl.class);

    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;

    private static String ret__success = "{\"code\":1,\"msg\":\"回调成功\"}";  //成功返回字符串
    private static String ret__failed = "{\"code\":0,\"msg\":\"回调失败\"}";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true
    private String time = String.valueOf(System.currentTimeMillis());

    public SIFPayServiceImpl() {
    }

    public SIFPayServiceImpl(Map<String, String> data) {
        if(MapUtils.isNotEmpty(data)){
                if(data.containsKey("merchId")){
                    this.merchId = data.get("merchId");
                }
                if(data.containsKey("payUrl")){
                    this.payUrl = data.get("payUrl");
                }
                if(data.containsKey("notifyUrl")){
                    this.notifyUrl = data.get("notifyUrl");
                }
                if(data.containsKey("secret")){
                    this.secret = data.get("secret");
                }
                if(data.containsKey("queryOrderUrl")){
                    this.queryOrderUrl = data.get("queryOrderUrl");
                }
        }
    }

    /**
     * 回调
     * @param request
     * @param response
     * @param config
     * @return
     */
    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        this.merchId = config.getString("merchId");
        this.notifyUrl = config.getString("notifyUrl");
        this.secret = config.getString("secret");
        this.queryOrderUrl = config.getString("queryOrderUrl");

        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);
        logger.info("[SIF]四方支付扫码支付回调请求参数：" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("SIFNotify获取回调请求参数为空");
            return ret__failed;
        }

        // 与 收的Header中sign值比对
        String sign = request.getHeader("sign");//取支付商返回签名
        String backTime = request.getHeader("timestamp");//取支付商返回签名
        dataMap.put("sign", sign);
        dataMap.put("timestamp", backTime);

        String trade_no = dataMap.get("orderno");//第三方订单号，流水号
        String order_no = dataMap.get("orderno");//支付订单号
        String amount = dataMap.get("amount");//两位小数（元）
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[SIF]四方支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }
        if (StringUtils.isBlank(amount) || amount == null) {
            logger.info("[SIF]四方支付扫码支付回调请求参数,订单金额为空");
            return ret__failed;
        }

        String trade_status = dataMap.get("status");  //订单状态 0失效，1未付款，5已付款
        String t_trade_status = "5";//第三方成功状态

        /**订单查询*/
        if (!orderQuery(order_no)) {
            logger.info("[SIF]四方支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }

        //写入数据库
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setRealAmount(Double.parseDouble(amount));
        processNotifyVO.setIp(ip);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setRet__success(ret__success);
        processNotifyVO.setInfoMap(JSONObject.fromObject(dataMap).toString());
        processNotifyVO.setT_trade_status(t_trade_status);
        processNotifyVO.setConfig(config);
        processNotifyVO.setPayment("SIF");

        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[SIF]四方支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    private Boolean orderQuery(String order_no){
        //订单查询
        try{
            JSONObject queryMap = new JSONObject();
            queryMap.put("orderno", order_no);//商户订单号

            String sign = generatorSign(queryMap, 1);

            logger.info("[SIF]四方支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));
            String ponse = toPostJsonStr(queryMap, queryOrderUrl, merchId, sign, time);

            if(StringUtils.isBlank(ponse)){
                logger.info("[SIF]四方支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            logger.info("[SIF]四方支付扫码支付回调查询订单{}响应信息：{}", order_no, JSONObject.fromObject(ponse));

            JSONObject jb = JSONObject.fromObject(ponse);
            //1：成功；2：失败
            if(jb.containsKey("code") && "1".equals(jb.getString("code"))){
                JSONObject json = jb.getJSONObject("data");
                //订单状态 0失效，1未付款，5已付款
                if("5".equals(json.getString("pay_status"))){
                    logger.info("[SIF]四方支付扫码支付回调查询订单,订单支付状态为:{}",json.getString("pay_status"));
                    return true;
                }else {
                    return false;
                }
            }else {
                logger.info("[SIF]四方支付扫码支付回调查询订单,请求状态为:{}",jb.getString("code"));
                return false;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[SIF]四方支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
            return false;
        }
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        return null;
    }

    /**
     * 扫码支付
     * @param payEntity
     * @return
     */
    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[SIF]四方支付扫码支付开始===============START========================");
        try{
            JSONObject dataMap = sealRequest(payEntity);
            String sign = generatorSign(dataMap, 1);

            String responseData = toPostJsonStr(dataMap, payUrl,merchId,sign,time);
            logger.info("[SIF]四方支付扫码支付响应信息：{}", responseData);
            if(StringUtils.isBlank(responseData)){
                logger.info("[SIF]四方支付发起HTTP请求无响应");
                return PayResponse.error("[SIF]四方支付扫码支付发起HTTP请求无响应");
            }
            JSONObject json = JSONObject.fromObject(responseData);
            if(json.containsKey("code") && "1".equalsIgnoreCase(json.getString("code"))){
                JSONObject data = json.getJSONObject("data");

                if(StringUtils.isBlank(payEntity.getMobile())){
                    return PayResponse.sm_qrcode(payEntity, data.getString("pay_url"), "下单成功");
                }
                return PayResponse.sm_link(payEntity, data.getString("pay_url"), "下单成功");

            }
            return PayResponse.error("下单失败,原因："+ json.getString("msg"));

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[SIF]四方支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[SIF]四方支付扫码支付异常");
        }
    }

    /**
     * 回调验签
     * @param data
     * @return
     */
    @Override
    public String callback(Map<String, String> data) {
        String sourceSign = data.remove("sign");

        TreeMap<String, String> treeMap = new TreeMap<>(data);
        String sign = generatorSign(JSONObject.fromObject(treeMap), 2);

        logger.info("[SIF]四方支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);

        if(sign.equals(sourceSign)){
            return "success";
        }
        return "fail";
    }
    /**
     * 组装参数
     * @param payEntity
     * @return
     */
    private  JSONObject sealRequest(PayEntity payEntity){
        JSONObject dataMap = new JSONObject();
        String amount = new DecimalFormat("0.00").format(payEntity.getAmount());

        dataMap.put("amount", amount);//金额,两位小数（元）
       // dataMap.put("clientGps","");//
        dataMap.put("notifyurl", notifyUrl);//通知回调地址
        dataMap.put("orderno", payEntity.getOrderNo());//商户订单号
        dataMap.put("pay_type",payEntity.getPayCode());//付款方式， 详见数据字典
        dataMap.put("remark", "TOP-UP");//备注

        logger.info("[SIF]四方支付扫码支付请求参数：{}", dataMap);
        return dataMap;
    }

    /**
     * 生成签名
     * @param data
     * @return
     */
    private String generatorSign( JSONObject data, int type){
        try{
            //签名方法【同时适用 回调通知类型接口】：
            //body参数(json格式)，key按字典顺序排序，然后按“key1=value2&key2=value2”组装生成 “字符串A”
            //“时间戳timestamp” 拼接 “接入密串app_secret” 拼接 “字符串A” ，生成新的“字符串B”
            //md5加密 “字符串B”，然后转换成大写，得到sign
            StringBuffer sb = new StringBuffer();
            if(1 == type){
                sb.append(time).append(secret);
            }else {//回调时，时间戳取支付商返回的值
                sb.append(data.get("timestamp")).append(secret);
            }
            
            Iterator<String> iterator = data.keySet().iterator();
            while (iterator.hasNext()){
                String key = iterator.next();
                String val = data.getString(key);
                if("sign".equalsIgnoreCase(key) || "timestamp".equalsIgnoreCase(key)|| "plat".equalsIgnoreCase(key)){
                    continue;
                }
                sb.append(key).append("=").append(val).append("&");
            }

            sb.replace(sb.length()-1,sb.length(),"");
            String strString = sb.toString();
            logger.info("[SIF]四方支付扫码支付生成待签名串：{}",strString);
            String sign = DigestUtils.md5Hex(sb.toString()).toUpperCase();

            logger.info("[SIF]四方支付扫码支付生成签名串：{}",sign);
            return sign;
        }catch (Exception e){
            e.getStackTrace();
            logger.info("[SIF]四方支付扫码支付生成签名串：{}",e.getMessage());
            return "扫码支付异常";
        }
    }
    /**
     *
     * @Description 发起参数为json类型的post请求
     * @param data
     * @param url
     * @return
     * @throws Exception
     */
    public static String toPostJsonStr(JSONObject data,String url, String merchId, String sign ,String time) throws Exception{
        CloseableHttpClient httpclient = null;
        try {
            httpclient = HttpClients.custom()
                    .setConnectionManager(createConnectionManager())
                    .build();
            HttpPost httppost = new HttpPost(url);
            if(data != null && !data.isEmpty()){

                httppost.setHeader("appid", merchId);
                httppost.setHeader("sign", sign);
                httppost.setHeader("timestamp", time);
                httppost.setHeader("Content-Type", "application/json");
                StringEntity entity = new StringEntity(data.toString(),"utf-8");//解决中文乱码问题
                httppost.setEntity(entity);
            }
            CloseableHttpResponse response = httpclient.execute(httppost);
            if(response.getStatusLine().getStatusCode() == 200){
                HttpEntity entity = response.getEntity();
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(),Consts.UTF_8));
                StringBuffer sb = new StringBuffer();
                String content = null;
                while((content = reader.readLine()) != null){
                    sb.append(content);
                }
                return sb.toString();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }finally {
            if(httpclient != null){
                httpclient.close();
            }
        }
    }

    private static PoolingHttpClientConnectionManager createConnectionManager() throws Exception {
        TrustManager tm = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { tm }, null);

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(context,
                NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.INSTANCE).register("https", socketFactory).build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                socketFactoryRegistry);
        return connectionManager;
    }
}
