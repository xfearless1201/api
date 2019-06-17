package com.cn.tianxia.api.pay.impl;


import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.dfu.utils.RSAUtils;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MapUtils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.vo.ProcessNotifyVO;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @ClassName   XJRPayServiceImpl
 * @Description 新金睿支付  渠道:支付宝扫码
 * @Author Vicky
 * @Version 1.0.0
 **/
public class DFUPayServiceImpl extends PayAbstractBaseService implements PayService {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(DFUPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String publicKey;//公钥
    private String queryOrderUrl;

    private static String ret__success = "OK";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true

    String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());


    public DFUPayServiceImpl() {
    }

    public DFUPayServiceImpl(Map<String, String> data) {
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
            if(data.containsKey("publicKey")){
                this.publicKey = data.get("publicKey");
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


        logger.info("[DFU]鼎付支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("DFUNotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("tradeNo");//第三方订单号，流水号
        String order_no = dataMap.get("orderNo");//支付订单号
        String amount = dataMap.get("actualPrice");//终端支付用户实际支付金额，依据此金额上分
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[DFU]鼎付支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        //0未处理、2处理失败、3处理中、6处理成功
        String trade_status = dataMap.get("orderStatus");  //第三方支付状态
        String t_trade_status = "1";//第三方成功状态

        /**订单查询*/
        if(!queryOrder(order_no)) {
            logger.info("[DFU]鼎付支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }

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
        processNotifyVO.setPayment("DFU");



        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[DFU]鼎付支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    private  Boolean queryOrder(String order_no){
        //订单查询
        try{

            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("brandNo", merchId);//
            queryMap.put("orderNo", order_no);//
            queryMap.put("signature", generatorSign(queryMap,3));//
            queryMap.put("signType", "RSA-S");//

            logger.info("[DFU]鼎付支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));

            String ponse =HttpUtils.toPostJsonStr(JSONObject.fromObject(queryMap), queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[DFU]鼎付支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            logger.info("[DFU]鼎付支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);

            if(jb.containsKey("isSuccess") && jb.getBoolean("isSuccess")){
                JSONObject json = jb.getJSONObject("data");

                  if("1".equals(json.getString("orderStatus")) || "4".equals(json.getString("orderStatus"))){
                      return true;
                  }else {
                      logger.info("[DFU]鼎付支付扫码支付回调查询订单{}交易状态：{}", order_no, json.getString("orderStatus"));
                      return false;
                  }
            }else {
                logger.info("[DFU]鼎付支付扫码支付回调查询订单{}响应描述：{}", order_no, jb.getString("message"));
                return false;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[DFU]鼎付支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
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
		logger.info("[DFU]鼎付支付扫码支付开始===================START=======================");
        try {
            JSONObject map = sealRequest(payEntity);
            logger.info("[DFU]鼎付支付扫码支付请求参数：{}", map);

            String responseData = HttpUtils.toPostJsonStr(JSONObject.fromObject(map), payUrl);
            logger.info("[DFU]鼎付支付扫码支付HTTP响应参数：{}", JSONObject.fromObject(responseData));

            JSONObject jb = JSONObject.fromObject(responseData);
            if(jb.containsKey("isSuccess") && jb.getBoolean("isSuccess")){
                JSONObject json = jb.getJSONObject("data");
                return PayResponse.sm_link(payEntity,json.getString("payUrl"),"下单成功");

            }
            return PayResponse.error("下单失败："+jb.get("message"));

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[DFU]鼎付支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[DFU]鼎付支付扫码支付异常");
        }
	}


    public String callback(Map<String, String> data) {
        logger.info("[DFU]鼎付支付回调验签开始===================START==============");
        try {

            StringBuffer sb = new StringBuffer();
            sb.append("actualPrice=").append(data.get("actualPrice")).append("&");
            sb.append("dealTime=").append(data.get("dealTime")).append("&");
            sb.append("orderNo=").append(data.get("orderNo")).append("&");
            sb.append("orderStatus=").append(data.get("orderStatus")).append("&");
            sb.append("orderTime=").append(data.get("orderTime")).append("&");
            sb.append("price=").append(data.get("price")).append("&");
            sb.append("tradeNo=").append(data.get("tradeNo"));

            boolean flag = RSAUtils.validateSignByPublicKey(sb.toString(),publicKey,data.get("signature"));
            if(flag){
                return ret__success;
            }else {
                return ret__failed;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[DFU]鼎付支付回调验签异常:"+e.getMessage());
            return ret__failed;
        }

    }
	
	/**
     * 
     * @Description 封装支付请求参数
     * @param entity
     * @param
     * @return
     * @throws Exception
     */
    private JSONObject sealRequest(PayEntity entity) throws Exception{
        logger.info("[DFU]鼎付支付组装支付请求参数开始===================START==================");
        try {

            String amount = new DecimalFormat("0.00").format(entity.getAmount());

            JSONObject dataMap = new JSONObject();
            dataMap.put("brandNo", merchId);//是,商户编号
            dataMap.put("callbackUrl", notifyUrl);//否,回调通知地址, 如果不传值, 则不通知
            dataMap.put("clientIP", entity.getIp());//是,商户端用户端IP
           // dataMap.put("frontUrl", "");//前台跳转地址, 如果不传值, 则不跳转
            dataMap.put("orderNo", entity.getOrderNo());//是,订单编号 (必须唯一)
            dataMap.put("price", amount);//是,交易金额（元）, 举例：12.00;小数点最多两位!
            dataMap.put("serviceType", entity.getPayCode());//是,服务类型 (见服务类型代码表)
            dataMap.put("signType", "RSA-S");//是,签名方式 (RSA-S)
            dataMap.put("userName", entity.getUsername());//是,商户端用户名 (交易进行者的身份识别码)
            dataMap.put("signature", generatorSign(dataMap, 1));//是,（即key1=value1&key2=value2…）拼接成字符串 [使用私钥]RSA-S加密: 把所有参数按字母顺序连接后进行 RSA-S 签章，并进行 Url Encode

            TreeMap<String,String> treeMap = new TreeMap<>(dataMap);


            return JSONObject.fromObject(treeMap);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[DFU]鼎付支付组装支付请求参数异常:"+e.getMessage());
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
	private String generatorSign(Map<String,String> data, int type ) throws Exception{
        logger.info("[DFU]鼎付支付生成签名串开始================START=================");
        try {

            StringBuffer sb = new StringBuffer();

            String sign = null;
            if(1 == type){
                sb.append("brandNo=").append(data.get("brandNo")).append("&");
                sb.append("clientIP=").append(data.get("clientIP")).append("&");
                sb.append("orderNo=").append(data.get("orderNo")).append("&");
                sb.append("price=").append(data.get("price")).append("&");
                sb.append("serviceType=").append(data.get("serviceType")).append("&");
                sb.append("userName=").append(data.get("userName"));

                sign = RSAUtils.signByPrivateKey(sb.toString(),secret);

            } else {
                //brandNo={brandNo}&orderNo={orderNo}
                sb.append("brandNo=").append(merchId).append("&");
                sb.append("orderNo=").append(data.get("orderNo"));
                sign= RSAUtils.signByPrivateKey(sb.toString(),secret);
            }
            logger.info("[DFU]鼎付支付扫码支付生成待签名串：{}",sb.toString()+";");
            logger.info("[DFU]鼎付支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[DFU]鼎付支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[DFU]鼎付支付生成签名串异常!");
        }
    }



}
