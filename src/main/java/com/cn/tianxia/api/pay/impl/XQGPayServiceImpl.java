package com.cn.tianxia.api.pay.impl;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.JSONUtils;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.vo.ProcessNotifyVO;

import net.sf.json.JSONObject;

/**
 * 
 * @ClassName XQGPayServiceImpl
 * @Description 新钱柜支付
 * @author Bing
 * @Date 2019年1月25日 下午12:05:19
 * @version 1.0.0
 */
public class XQGPayServiceImpl extends PayAbstractBaseService implements PayService {
    
    //日志
    private static final Logger logger = LoggerFactory.getLogger(XQGPayServiceImpl.class);
    /**回调失败响应信息*/
    private static final String ret__failed = "fail";
    /**回调成功响应信息*/
    private static final String ret__success = "success";
    /**商户号*/
    private String merchId;
    /**秘钥*/
    private String secret;
    /**回调地址*/
    private String notifyUrl;
    /**支付地址*/
    private String payUrl;
    /**订单查询地址*/
    private String queryOrderUrl;
    /**网银通道Id*/
    private String wyChannelId;
    
    public XQGPayServiceImpl() {}
    //构造器,初始化参数
    public XQGPayServiceImpl(Map<String,String> data) {
        if(MapUtils.isNotEmpty(data)){
            if(data.containsKey("merchId")){
                this.merchId = data.get("merchId");
            }
            if(data.containsKey("payUrl")){
                this.payUrl = data.get("payUrl");
            }
            if(data.containsKey("queryOrderUrl")){
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
            if(data.containsKey("notifyUrl")){
                this.notifyUrl = data.get("notifyUrl");
            }
            if(data.containsKey("secret")){
                this.secret = data.get("secret");
            }
            if(data.containsKey("wyChannelId")){
                this.wyChannelId = data.get("wyChannelId");
            }
        }
        
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        try {
            Map<String, String> data = sealRequest(payEntity, 2);
            logger.info("[XQG]新钱柜网银支付请求参数：{}", JSONObject.fromObject(data));
            String resStr = HttpUtils.toPostForm(data, payUrl +"unionPayH5" );
            logger.info("[YXIN]网银网银支付响应信息：{},支付地址：{}", JSONObject.fromObject(resStr), payUrl+"unionPayH5");
            if (StringUtils.isBlank(resStr)) {
                logger.info("[XQG]新钱柜网银支付发起HTTP请求无响应结果");
                return PayResponse.error("[XQG]新钱柜网银支付扫码支付发起HTTP请求无响应结果");
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if (JSONUtils.compare(resJson, "code", "0")) {
                String qcurl = resJson.getJSONObject("data").getString("url");

                return PayResponse.wy_link(qcurl);
            }
            return PayResponse.error("[XQG]新钱柜网银支付下单失败:" + resJson.getString("msg"));
        }catch (Exception e){
            e.printStackTrace();
            logger.info("[XQG]新钱柜网银支付异常:{}",e.getMessage());
            return PayResponse.error("[XQG]新钱柜网银支付扫码支付异常");
        }
    }

    @Override
    public JSONObject smPay(PayEntity payEntity) {
        try {
            Map<String,String> data = sealRequest(payEntity, 1);
            logger.info("[XQG]新钱柜扫码支付请求参数：{}", JSONObject.fromObject(data));
            String resStr = HttpUtils.toPostForm(data, payUrl+payEntity.getPayCode());
            logger.info("[XQG]新钱柜扫码支付响应信息：{},支付地址：{}", JSONObject.fromObject(resStr),payUrl+payEntity.getPayCode());
            if(StringUtils.isBlank(resStr)){
                logger.info("[XQG]新钱柜扫码支付发起HTTP请求无响应结果");
                return PayResponse.error("[XQG]新钱柜扫码支付扫码支付发起HTTP请求无响应结果");
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if(JSONUtils.compare(resJson, "code", "0")){
                String qcurl = resJson.getJSONObject("data").getString("url");
                if(StringUtils.isBlank(payEntity.getMobile())&&payEntity.getPayCode().contains("native")) {
                    return PayResponse.sm_qrcode(payEntity, qcurl, "支付下单成功");
                }
                return PayResponse.sm_link(payEntity, qcurl, "支付下单成功");
            }
            return PayResponse.error("[XQG]新钱柜扫码支付下单失败:"+resJson.getString("msg"));
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XQG]新钱柜扫码支付异常:{}",e.getMessage());
            return PayResponse.error("[XQG]新钱柜扫码支付扫码支付异常");
        }
    }

    @Override
    public String callback(Map<String, String> data) {
        return null;
    }
    
    /**
     * 
     * @Description 组装支付请求参数
     * @param
     * @return
     * @throws Exception
     */
    private Map<String,String> sealRequest(PayEntity payEntity, int type) throws Exception{
            String amount = new DecimalFormat("0.00").format(payEntity.getAmount());
            Map<String,String> data = new HashMap<>();
            data.put("merchantName",merchId);//商户号
            if(2== type){
                data.put("channelId",wyChannelId);//通道ID,如果不传自动获取通道
            }
            data.put("orderId",payEntity.getOrderNo());//订单号
            data.put("amount",amount);//支付金额
            data.put("noticeUrl",notifyUrl);//回调地址
            data.put("sign",generatorSign(data, "0"));//回调地址
            data.put("signType","MD5");//加密类型
            return data;
    }

    /**
     * 
     * @Description 生成签名
     * @param data
     * @return
     * @throws Exception
     */
    private String generatorSign(Map<String,String> data, String type) throws Exception{
            StringBuilder sb = new StringBuilder();
            if("0".equals(type)||"1".equals(type)) {
                Map<String,String> map = new TreeMap<>(data);
                Iterator<String> iterator = map.keySet().iterator();
                while(iterator.hasNext()){
                    String key = iterator.next();
                    String val = map.get(key);
                    if(StringUtils.isBlank(val) || "sign".equalsIgnoreCase(key) ) continue;//|| "channelId".equalsIgnoreCase(key)
                    sb.append(key).append("=").append(val).append("&");
                }
            }
            if("2".equals(type)) {
               sb.append("amount=").append(data.get("amount")).append("&");
               sb.append("orderId=").append(data.get("orderId")).append("&");
            }
            sb.append("key=").append(secret);
            String signStr = sb.toString();
            logger.info("[XQG]新钱柜扫码支付生成待签名串:{}",signStr);
            String sign = MD5Utils.md5(signStr.getBytes());
            logger.info("[XQG]新钱柜扫码支付生成签名串:{}",sign);    
            return sign;
    }

    /**
     * 订单查询接口
     * @Description (TODO这里用一句话描述这个方法的作用)
     * @param orderNo
     * @return
     */
    public boolean serchOrder(String orderNo) {
        try {
            Map<String, String> param = new HashMap<>();
            param.put("merchantName", merchId);//商户号
            param.put("orderId", orderNo);//商户订单号
            param.put("sign", generatorSign(param, "1"));
            param.put("signType", "MD5");//
            logger.info("[XQG]新钱柜扫码支付回调查询订单{}请求参数：{}", orderNo, JSONObject.fromObject(param));
            String resStr = HttpUtils.toPostForm(param, queryOrderUrl);
            logger.info("[XQG]新钱柜扫码支付回调查询订单{}响应信息：{}", orderNo, JSONObject.fromObject(resStr));
            if(StringUtils.isBlank(resStr)) {
                logger.info("[XQG]新钱柜扫码支付回调查询订单发起HTTP请求无响应,订单号{}",orderNo);
                return false;
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if(!"0".equals(resJson.getString("code"))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XQG]新钱柜扫码支付回调查询订单{}异常{}", orderNo, e.getMessage());
            return false;
        }
        
    }
     /**
     * 回调验签
     * @Description (TODO这里用一句话描述这个方法的作用)
     * @param data
     * @return
     */
    private boolean verifyCallback(Map<String, String> data) {
        try {
            String sourceSign = data.get("sign");
            String sign = generatorSign(data, "2");
            logger.info("[XQG]新钱柜扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            return sourceSign.equalsIgnoreCase(sign);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XQG]新钱柜扫码支付回调生成签名串异常{}",e.getMessage());
            return false;
        }
    }
    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        Map<String,String> infoMap = ParamsUtils.getNotifyParams(request);  //获取回调请求参数
        logger.info("[XQG]新钱柜扫码支付回调请求参数："+JSONObject.fromObject(infoMap));
        if (MapUtils.isEmpty(infoMap)) {
            logger.error("XQGNotify获取回调请求参数为空");
            return ret__failed;
        }
        //参数验签
        this.secret = config.getString("secret");//从配置中获取
        this.merchId = config.getString("merchId");//从配置中获取
        this.queryOrderUrl = config.getString("queryOrderUrl");//从配置中获取
        
        String order_amount = infoMap.get("amount");//单位：元
        if(StringUtils.isBlank(order_amount)){
            logger.info("XQGNotify获取实际支付金额为空!");
            return ret__failed;
        }
        double realAmount = Double.parseDouble(order_amount);
        String order_no = infoMap.get("orderId");// 平台订单号
        String trade_no = "XQG"+System.currentTimeMillis();// 第三方订单号
        String trade_status = "00";//订单状态
        String t_trade_status = trade_status;// 表示成功状态
        
        /**订单查询*/
        if(!serchOrder(order_no)) {
            logger.info("[XQG]新钱柜扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }
        /**回调验签*/
        boolean verifyRequest = verifyCallback(infoMap);
        
        String ip = StringUtils.isBlank(IPTools.getIp(request))?"127.0.0.1":IPTools.getIp(request);
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setRet__success(ret__success);    //成功返回
        processNotifyVO.setRet__failed(ret__failed);      //失败返回
        processNotifyVO.setIp(ip);
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);    //支付状态
        processNotifyVO.setT_trade_status(t_trade_status);     //第三方成功状态
        processNotifyVO.setRealAmount(realAmount);
        processNotifyVO.setInfoMap(JSONObject.fromObject(infoMap).toString());    //回调参数
        processNotifyVO.setPayment("YXIN");
        processNotifyVO.setConfig(config);
        return super.processSuccessNotify(processNotifyVO,verifyRequest);
    }
}
