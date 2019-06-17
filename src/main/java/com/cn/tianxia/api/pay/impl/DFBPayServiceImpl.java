package com.cn.tianxia.api.pay.impl;


import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;
import com.cn.tianxia.api.utils.pay.MapUtils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.vo.ProcessNotifyVO;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName   DFBPayServiceImpl
 * @Description 德付宝支付  渠道:支付宝扫码
 * @Author Vicky
 * @Version 1.0.0
 **/
public class DFBPayServiceImpl extends PayAbstractBaseService implements PayService {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(DFBPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true

    String time = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date());//yyyy-MM-dd HH:mm:ss

    public DFBPayServiceImpl() {
    }

    public DFBPayServiceImpl(Map<String, String> data) {
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
        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("DFBNotify获取回调请求参数为空");
            return ret__failed;
        }

        JSONObject json = JSONObject.fromObject(dataMap);
        logger.info("[DFB]德付宝支付扫码支付回调请求参数：{}",JSONObject.fromObject(dataMap));
        logger.info("[DFB]德付宝支付扫码支付回调请求参数转JSON后：{}",dataMap);

        String jsonStr = json.getString("data");
        //字符串转JSON取值
        JSONObject result = JSONObject.fromObject(jsonStr);

        String trade_no = result.getString("lsh");//第三方无流水号
        String order_no = result.getString("lsh");//支付订单号
        String amount = result.getString("money");//终端支付用户实际支付金额，依据此金额上分
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[XJR]新金睿支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        //
        String trade_status = result.getString("stud");  //第三方支付状态
        String t_trade_status = "2";//第三方成功状态


        //写入数据库
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setRealAmount(Double.parseDouble(amount));//以“元”为单位
        processNotifyVO.setIp(ip);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setRet__success(ret__success);
        processNotifyVO.setInfoMap(JSONObject.fromObject(dataMap).toString());
        processNotifyVO.setT_trade_status(t_trade_status);
        processNotifyVO.setConfig(config);
        processNotifyVO.setPayment("DFB");

        /**订单查询*/
        if(!queryOrder(order_no)) {
            logger.info("[DFB]德付宝支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }

        //回调验签
        if ("fail".equals(callback(result))) {
            verifySuccess = false;
            logger.info("[DFB]德付宝支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    private  Boolean queryOrder(String order_no){
        //订单查询
        try{

            JSONObject queryMap = new JSONObject();
            queryMap.put("lsh", order_no);//
            queryMap.put("time", time);//
            queryMap.put("user", merchId);//
            queryMap.put("ch", generatorSign(queryMap, 2 ));//

            logger.info("[DFB]德付宝支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));
            Map<String, String> json = new HashMap<>();
            json.put("data", queryMap.toString());

           String ponse = HttpUtils.toPostWeb(json,queryOrderUrl);

           if(StringUtils.isBlank(ponse)){
                logger.info("[DFB]德付宝支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            logger.info("[DFB]德付宝支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);
            //0.支付中 1.失败 2.成功
            if(jb.containsKey("stud") && "2".equalsIgnoreCase(jb.getString("stud"))){
                return true;
            }else {
                logger.info("[DFB]德付宝支付扫码支付回调查询订单{}响应描述：{}", order_no, jb.getString("stud"));
                return false;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[DFB]德付宝支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
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
        logger.info("[DFB]德付宝支付扫码支付开始===================START=======================");
        try {
            Map<String, String> data = sealRequest(payEntity);
            logger.info("DFB]德付宝支付扫码支付请求参数：{}", JSONObject.fromObject(data));
            String responseData = HttpUtils.toPostWeb(data, payUrl);
            logger.info("[DFB]德付宝支付扫码支付HTTP响应参数：{}", responseData);
            JSONObject jb = JSONObject.fromObject(responseData);
            if(jb.containsKey("stud") && "0".equals(jb.getString("stud"))){
                return PayResponse.sm_link(payEntity,jb.getString("url"),"下单成功");
            }
            return PayResponse.error("下单失败：" + jb.get("msg"));

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[DFB]德付宝支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[DFB]德付宝支付扫码支付异常");
        }
    }


    public String callback(Map<String, String> data) {
        logger.info("[DFB]德付宝支付回调验签开始===================START==============");
        try {
            JSONObject reqJson = JSONObject.fromObject(data);
            //获取验签原串
            String sourceSign = data.remove("ch");
            //生成待签名串
            String sign = generatorSign(reqJson,3);
            logger.info("[DFB]德付宝支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            
            if(sourceSign.equalsIgnoreCase(sign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[DFB]德付宝支付回调验签异常:"+e.getMessage());
            return ret__failed;
        }
        return ret__failed;
    }

    /**
     *
     * @Description 封装支付请求参数
     * @param entity
     * @param
     * @return
     * @throws Exception
     */
    private Map<String, String> sealRequest(PayEntity entity) throws Exception{
        logger.info("[DFB]德付宝支付组装支付请求参数开始===================START==================");
        try {
            String amount = new DecimalFormat("##").format(entity.getAmount());
            JSONObject dataMap = new JSONObject();
            dataMap.put("lsh", entity.getOrderNo());//订单号
            dataMap.put("money", amount);//金额
            dataMap.put("user", merchId);//商户号
            dataMap.put("time",time);//现在时间 格式为（Y-m-d H:i:s）
            dataMap.put("type", entity.getPayCode());//1.微信 2.支付宝
            dataMap.put("reurl", entity.getRefererUrl());//支付后跳转页面
            dataMap.put("okreurl", notifyUrl);//异步通知
            dataMap.put("ch", generatorSign(dataMap, 1));//验证码=
            HashMap<String, String> reqData = new HashMap<>();
            reqData.put("data", dataMap.toString());
            return reqData;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[DFB]德付宝支付组装支付请求参数异常:"+e.getMessage());
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
    private String generatorSign(JSONObject data, int type ) throws Exception{
        logger.info("[DFB]德付宝支付生成签名串开始================START=================");
        try {

            StringBuffer sb = new StringBuffer();
            if(1== type){//支付
                //md5(lsh+money+user+time+type+reurl+okreurl+key)
                sb.append(data.get("lsh")).append(data.get("money")).append(merchId)
                        .append(data.get("time")).append(data.get("type")).append(data.get("reurl"))
                        .append(data.get("okreurl")).append(secret);
            }else if(2 == type){//查询
                //lsh+time+user+key
                sb.append(data.get("lsh")).append(data.get("time"))
                        .append(merchId).append(secret);

            }else {//回调
                //lsh+money+stud+key
                sb.append(data.get("lsh")).append(data.get("money"))
                        .append(data.get("stud")).append(secret);
            }


            logger.info("[DFB]德付宝支付扫码支付生成待签名串：{}",sb.toString());
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString()).toLowerCase();

            logger.info("[DFB]德付宝支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[DFB]德付宝支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[DFB]德付宝支付生成签名串异常!");
        }
    }
    /**
     *
     * @Description 发起流参数
     * @param
     * @param data
     * @return
     * @throws Exception
     */
    public static String toPostJsonStr(String data) throws Exception{
        String urlString = "http://dfbbpay.com/api2.php?type=add";
        String bodyString = "data="+data;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(bodyString.getBytes("utf-8"));
        os.flush();
        os.close();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } else {
            return String.valueOf(conn.getResponseCode());
        }
    }
}
