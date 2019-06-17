package com.cn.tianxia.api.pay.impl;

import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.pay.MD5Utils;
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
import java.util.*;

/**
 * @ClassName   EJPayServiceImpl
 * @Description E家支付  渠道：微信 渠道
 * @Author Vicky
 * @Version 1.0.0
 **/
public class EJPayServiceImpl extends PayAbstractBaseService implements PayService {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(EJPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;

    private static String ret__success = "ok";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true


    public EJPayServiceImpl() {
    }

    public EJPayServiceImpl(Map<String, String> data) {
        if(MapUtils.isNotEmpty(data)){
            if(data.containsKey("merchId")){
                this.merchId = data.get("merchId");
            }if(data.containsKey("payUrl")){
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
     */
    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        this.merchId = config.getString("merchId");
        this.notifyUrl = config.getString("notifyUrl");
        this.secret = config.getString("secret");
        this.queryOrderUrl = config.getString("queryOrderUrl");

        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);
        // 与 收的Header中sign值比对
        String sign = request.getHeader("sign");//取支付商返回签名
        dataMap.put("sign",sign);

        logger.info("[EJ]E家支付扫码支付回调请求参数：" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("EJNotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("order_id");//第三方订单号，流水号
        String order_no = dataMap.get("order_no");//支付订单号
        String amount = dataMap.get("amount");//终端支付用户实际支付金额，依据此金额上分
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[EJ]E家支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        //0：未支付 1：支付成功 2：失败
        String trade_status = dataMap.get("paid");  //第三方支付状态，1 支付成功
        String t_trade_status = "true";//第三方成功状态

        /**订单查询*/
        if(!queryOrder(order_no)) {
            logger.info("[EJ]E家支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }

        //写入数据库
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setRealAmount(Double.parseDouble(amount)/100);//以“分”为单位
        processNotifyVO.setIp(ip);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setRet__success(ret__success);
        processNotifyVO.setInfoMap(JSONObject.fromObject(dataMap).toString());
        processNotifyVO.setT_trade_status(t_trade_status);
        processNotifyVO.setConfig(config);
        processNotifyVO.setPayment("EJ");

        Map<String, Object> map = new HashMap<>(dataMap);
        map.put("amount", Integer.parseInt(dataMap.get("amount")));
        map.put("paid",true);
        map.put("time_settle", Long.parseLong(dataMap.get("time_settle")));
        map.put("time_paid",  Long.parseLong(dataMap.get("time_paid")));

        logger.info("回调验签前的参数："+String.valueOf(map));

        //回调验签
        if ("fail".equals(back(map))) {
            verifySuccess = false;
            logger.info("[EJ]E家支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    private  Boolean queryOrder(String order_no){
        //订单查询
        try{
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("mch_id", merchId);
            queryMap.put("order_no", order_no);
            queryMap.put("nonce_str", UUID.randomUUID().toString());

            logger.info("[EJ]E家支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));
            String querySign = generatorSign(queryMap);

            String ponse = toPostJsonStr(JSONObject.fromObject(queryMap), queryOrderUrl,querySign);

            if(StringUtils.isBlank(ponse)){
                logger.info("[EJ]E家支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            logger.info("[EJ]E家支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);

            if("ok".equalsIgnoreCase(jb.getString("state"))){
                JSONObject json = jb.getJSONObject("data") ;
                if(json.containsKey("paid") && json.getBoolean("paid")){
                    return true;
                }else {
                    logger.info("[EJ]E家支付扫码支付回调查询订单{}支付状态：{}", order_no, json.getBoolean("paid"));
                    return false;
                }
            }else {
                logger.info("[EJ]E家支付扫码支付回调查询订单{}请求状态：{}", order_no, jb.getString("state"));
                return false;
            }



        }catch (Exception e){
            e.getStackTrace();
            logger.info("[EJ]E家支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
            return false;
        }
    }
    /**
     * 网银支付
     */
    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        return null;
    }

    /**
     * 扫码支付
     */
    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[EJ]E家支付扫码支付开始===================START=======================");
        try {
            Map<String,String> map = sealRequest(payEntity);
            logger.info("[EJ]E家支付扫码支付请求参数：{}", JSONObject.fromObject(map));
            String sign = generatorSign(map);
            String responseData = toPostJsonStr(JSONObject.fromObject(map), payUrl,sign);

            logger.info("[EJ]E家支付扫码支付HTTP响应参数：{}", JSONObject.fromObject(responseData));

            JSONObject jb = JSONObject.fromObject(responseData);
            if(jb.containsKey("state") && "ok".equalsIgnoreCase(jb.getString("state"))){
                JSONObject json = jb.getJSONObject("data").getJSONObject("credential");
                return PayResponse.sm_link(payEntity,json.getString("pay_url"),"下单成功");

            }
            return PayResponse.error("下单失败");

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[EJ]E家支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[EJ]E家支付扫码支付异常");
        }
    }



    public String callback(Map<String, String> data) {
        return null;
    }

    /**
     *
     * @Description 封装支付请求参数
     * @param entity
     * @param
     * @return
     * @throws Exception
     */
    private Map<String,String> sealRequest(PayEntity entity) throws Exception{
        logger.info("[EJ]E家支付组装支付请求参数开始===================START==================");
        try {

            String amount = new DecimalFormat("##").format(entity.getAmount() *100);
            //创建存储支付请求参数对象
            Map<String,String> dataMap = new HashMap<>();

            dataMap.put("mch_id", merchId);//平台分配的商户号
            dataMap.put("order_no", entity.getOrderNo());//商户订单号，适配每个渠道对此参数的要求，必须在商户的系统内唯一。注： 推荐使用 8-32 位的数字和字母组合，不允许特殊字符
            dataMap.put("channel", entity.getPayCode());//支付使用的第三方支付渠道。
            dataMap.put("amount", amount);//订单总金额（如果为0表示充值模式，实际到账金额以支付结果回调通知中amount_real字段为准），单位为对应币种的最小货币单位，人民币为分。如订单总金额为 1 元，
            // dataMap.put("currency", "cny");//默认为人民币 cny
            dataMap.put("client_ip", entity.getIp());//发起支付请求客户端的 IPv4 地址，如: 127.0.0.1
            dataMap.put("subject", "TOP-UP");//商品标题，该参数最长为 32 个 Unicode 字符
            dataMap.put("body", "recharge");//商品描述信息，该参数最长为 128 个 Unicode 字符
            dataMap.put("notify_url", notifyUrl);//异步接收支付结果通知的回调地址，地址必须为外网可访问的url。 参见 结果通知


            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[EJ]E家支付组装支付请求参数异常:"+e.getMessage());
            throw new Exception("组装支付请求参数异常!");
        }
    }


    /**
     *
     * @Description 生成签名串
     * @param data
     * @return
     * @throws Exception
     */
    private String generatorSign(Map<String,String> data ) throws Exception{
        logger.info("[EJ]E家支付生成签名串开始================START=================");
        try {
            String charge = String.valueOf(JSONObject.fromObject(data));

            logger.info("[EJ]E家支付扫码支付生成待签名串：{}",charge);
            String sign = MD5Utils.md5toUpCase_32Bit(charge+secret);

            logger.info("[EJ]E家支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[EJ]E家支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[EJ]E家支付生成签名串异常!");
        }
    }
    /**
     * 异步回调接口
     */
    public String back(Map<String, Object> data) {
        logger.info("[EJ]E家支付回调验签开始===================START==============");
        try {
            //获取验签原串
            String sourceSign = String.valueOf(data.remove("sign"));

            //生成待签名串
            String sign = callSign(data);
            logger.info("[EJ]E家支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            //验签
            if(sourceSign.equalsIgnoreCase(sign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[EJ]E家支付回调验签异常:"+e.getMessage());
            return ret__failed;
        }
        return ret__failed;
    }
    private String callSign(Map<String,Object> data ) throws Exception{
        logger.info("[EJ]E家支付回调生成签名串开始================START=================");
        try {
            JSONObject json = new JSONObject();
            json.put("amount",data.get("amount"));
            json.put("body",data.get("body"));
            json.put("channel",data.get("channel"));
            json.put("client_ip",data.get("client_ip"));
            json.put("currency",data.get("currency"));
            json.put("extra",   "\"{}\"");
            json.put("mch_id",data.get("mch_id"));
            json.put("order_id",data.get("order_id"));
            json.put("order_no",data.get("order_no"));
            json.put("paid",data.get("paid"));
            json.put("subject",data.get("subject"));
            json.put("time_paid",data.get("time_paid"));
            json.put("time_settle",data.get("time_settle"));
            json.put("transaction_no",data.get("transaction_no"));

            String charge = String.valueOf(json);

            logger.info("[EJ]E家支付扫码支付回调生成待签名串：{}",charge+secret);
            String sign = MD5Utils.md5toUpCase_32Bit(charge+secret);

            logger.info("[EJ]E家支付扫码支付回调生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[EJ]E家支付扫码支付回调生成签名串异常：{}",e.getMessage());
            throw new Exception("[EJ]E家支付回调生成签名串异常!");
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
    public static String toPostJsonStr(JSONObject data,String url,String sign) throws Exception {
        CloseableHttpClient httpclient = null;
        try {
            httpclient = HttpClients.custom()
                    .setConnectionManager(createConnectionManager())
                    .build();
            HttpPost httppost = new HttpPost(url);
            if (data != null && !data.isEmpty()) {
                StringEntity entity = new StringEntity(data.toString(), "utf-8");//解决中文乱码问题
                httppost.setEntity(entity);
                httppost.setHeader("sign", sign);
                httppost.setHeader("Content-Type", "application/json");
            }
            CloseableHttpResponse response = httpclient.execute(httppost);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), Consts.UTF_8));
                StringBuffer sb = new StringBuffer();
                String content = null;
                while ((content = reader.readLine()) != null) {
                    sb.append(content);
                }
                return sb.toString();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (httpclient != null) {
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
