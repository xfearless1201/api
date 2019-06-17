package com.cn.tianxia.api.pay.impl;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.utils.JSONUtils;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.utils.pay.XmlUtils;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.vo.ProcessNotifyVO;

import net.sf.json.JSONObject;

/**
 *  * @ClassName KPAYPayServiceImpl
 *  * @Description TODO(kpay支付)
 *  * @Author Bing
 *  * @Date 2019年05月04日 20:10
 *  * @Version 1.0.0
 *  
 **/
public class KPAYPayServiceImpl extends PayAbstractBaseService implements PayService {
    private static final Logger logger = LoggerFactory.getLogger(KPAYPayServiceImpl.class);
    /**
     * 回调失败响应信息
     */
    private static final String ret__failed = "fail";
    /**
     * 回调成功响应信息
     */
    private static final String ret__success = "success";
    /**
     * 商户号
     */
    private String merchId;
    /**
     * 秘钥
     */
    private String secret;
    /**
     * 回调地址
     */
    private String notifyUrl;
    /**
     * 支付地址
     */
    private String payUrl;
    /**
     * 网银支付地址
     */
    private String wyPayUrl;
    /**
     * 订单查询地址
     */
    private String queryOrderUrl;

    public KPAYPayServiceImpl() {
    }

    public KPAYPayServiceImpl(Map<String, String> data) {
        if (MapUtils.isNotEmpty(data)) {
            if (data.containsKey("merchId")) {
                this.merchId = data.get("merchId");
            }
            if (data.containsKey("secret")) {
                this.secret = data.get("secret");
            }
            if (data.containsKey("notifyUrl")) {
                this.notifyUrl = data.get("notifyUrl");
            }
            if (data.containsKey("payUrl")) {
                this.payUrl = data.get("payUrl");
            }
            if (data.containsKey("wyPayUrl")) {
                this.wyPayUrl = data.get("wyPayUrl");
            }
            if (data.containsKey("queryOrderUrl")) {
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
        }
    }

    /**
     * @param payEntity
     * @return
     * @Description 网银支付
     */
    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        try {
            Map<String, String> data = sealRequest(payEntity, "0");
            String reqXml = getReqXml(data);
            Map<String, String> reqData = new HashMap<>();
            reqData.put("req_data", reqXml);
            logger.info("[KPAY]kpay网银支付json请求参数：{}", JSONObject.fromObject(reqData));
            String resStr = HttpUtils.generatorForm(reqData, wyPayUrl);
            logger.info("[KPAY]kpay扫码支付响应信息：{}", resStr);
            return PayResponse.wy_form(payEntity.getPayUrl(), resStr);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[KPAY]kpay网银支付异常:{}", e.getMessage());
            return PayResponse.error("[KPAY]kpay网银支付异常");
        }
    }

    /**
     * @param
     * @return
     * @Description 扫码支付
     */
    @Override
    public JSONObject smPay(PayEntity payEntity) {
        try {
            //获取支付请求参数
            Map<String, String> data = sealRequest(payEntity,"1");
            String reqXml = getReqXml(data);
            logger.info("[KPAY]kpay支付扫码支付json请求参数:{}--xml请求参数：{}", JSONObject.fromObject(data), reqXml);
            String resStr = null;
            if(StringUtils.isNotBlank(payEntity.getMobile())) {
                Map<String, String> reqData = new HashMap<>();
                reqData.put("req_data", reqXml);
                resStr = HttpUtils.generatorForm(reqData, payUrl+"wapPay");
                return PayResponse.sm_form(payEntity, resStr, "下单成功");
            }else {
                resStr = HttpUtils.toPostXml(reqXml, payUrl+"nativePay");
            }
            logger.info("[KPAY]kpay支付扫码支付响应信息：{}", resStr);
            if (StringUtils.isBlank(resStr)) {
                logger.info("[KPAY]kpay支付扫码支付下单失败，无响应结果");
                return PayResponse.error("[KPAY]kpay支付扫码支付下单失败，无响应结果");
            }
            JSONObject resJson = XmlUtils.xml2Json(resStr);
            if(JSONUtils.compare(resJson, "retcode", "0")) {
                return PayResponse.sm_link(payEntity, resJson.getString("codeImgUrl"), "下单成功");
            }
            return PayResponse.error("下单失败："+resJson.getString("retmsg"));

        } catch (Exception e) {
            e.printStackTrace();
            return PayResponse.error("[KPAY]kpay支付扫码支付下单异常" + e.getMessage());
        }
    }


    /**
     * @param data
     * @return
     * @Description 回调验签
     */
    @Override
    public String callback(Map<String, String> data) {
        return null;
    }

    /**
     * @param
     * @return
     * @throws Exception
     * @Description 组装支付请求参数
     */
    private Map<String, String> sealRequest(PayEntity payEntity, String type) throws Exception {
        String amount = new DecimalFormat("0").format(payEntity.getAmount()*100);
        Map<String, String> data = new HashMap<>();
        data.put("version", "2.0");//版本号
        data.put("charset", "UTF-8");//字符集
        data.put("spid", merchId);//商户号
        data.put("spbillno", payEntity.getOrderNo());//订单号
        data.put("tranAmt", amount);//金额 单位：分
        if("0".equals(type)) {
            data.put("cardType", "0");//0:借记卡 1：贷记卡
            data.put("bankCode", payEntity.getPayCode());//银行代号
        }
        if("1".equals(type)) {
            data.put("payType", "pay."+payEntity.getPayCode());//银行代号
        }
        
        data.put("backUrl", payEntity.getRefererUrl());//通知地址
        data.put("notifyUrl", notifyUrl);//通知地址
        data.put("productName", "recharge");//备注
        data.put("sign", generatorSign(data));//签名
        data.put("signType", "MD5");//备注
        return data;
    }

    /**
     * @param data
     * @return
     * @throws Exception
     * @Description 生产签名串
     */
    private String generatorSign(Map<String, String> data) throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            Map<String, String> map = new TreeMap<>(data);
            Iterator<String> iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String val = map.get(key);
                if (StringUtils.isBlank(val) || "sign".equalsIgnoreCase(key)) {
                    continue;
                }
                sb.append(key).append("=").append(val).append("&");
            }
            sb.append("key=").append(secret);
            String signStr = sb.toString();
            logger.info("[KPAY]kpay支付生成待签名串:" + signStr);
            String sign = MD5Utils.md5toUpCase_32Bit(signStr);
            logger.info("[KPAY]kpay支付生成MD5加密签名串:" + sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[KPAY]kpay支付生产签名串异常:" + e.getMessage());
            throw new Exception("生产MD5签名串失败!");
        }
    }

    /**
     * 订单查询接口
     *
     * @param orderNo
     * @return
     * @Description (TODO这里用一句话描述这个方法的作用)
     */
    public boolean serchOrder(String orderNo, String tradeNo) {
        try {
            Map<String, String> param = new HashMap<>();
            param.put("version", "2.0");//版本号
            param.put("charset", "UTF-8");//字符集
            param.put("spid", merchId);//商户号
            param.put("transactionId", tradeNo);//k-pay 单号
            param.put("spbillno", orderNo);//商户订单号
            param.put("sign", generatorSign(param));
            param.put("signType", "MD5");
            String reqXml = getReqXml(param);
            logger.info("[KPAY]kpay支付回调查询订单"+orderNo+"json请求参数：{}-\n-xml请求参数：{}", JSONObject.fromObject(param), reqXml);
            String resStr = HttpUtils.toPostXml(reqXml, queryOrderUrl);
            logger.info("[KPAY]kpay支付回调查询订单{}响应信息：{}", orderNo, resStr);
            if (StringUtils.isBlank(resStr)) {
                logger.info("[KPAY]kpay支付回调查询订单发起HTTP请求无响应,订单号{}", orderNo);
                return false;
            }
            JSONObject resJson = XmlUtils.xml2Json(resStr);
            logger.info("[KPAY]kpay支付回调查询订单"+orderNo+"xml响应信息：{}-\n-json响应信息：{}", resStr, resJson);
            if(!JSONUtils.compare(resJson, "retcode", "0")) {
                return false;
            }
            if(!JSONUtils.compare(resJson, "result", "pay_success")&&!JSONUtils.compare(resJson, "result", "pay_processing")) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[KPAY]kpay支付回调查询订单{}异常{}", orderNo, e.getMessage());
            return false;
        }

    }

    /**
     * 回调验签
     *
     * @param data
     * @return
     * @Description (TODO这里用一句话描述这个方法的作用)
     */
    private boolean verifyCallback(Map<String, String> data) {
        try {
            data.remove("attach");
            data.remove("signType");
            String sourceSign = data.remove("sign");
            String sign = generatorSign(data);
            logger.info("[KPAY]kpay支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            return sourceSign.equalsIgnoreCase(sign);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[KPAY]kpay支付回调生成签名串异常{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        Map<String, String> infoMap = ParamsUtils.getKPAYNotifyParams(request);  //获取回调请求参数
        logger.info("[KPAY]kpay支付回调请求参数：" + JSONObject.fromObject(infoMap));
        if (MapUtils.isEmpty(infoMap)) {
            logger.error("KPAYNotify获取回调请求参数为空");
            return ret__failed;
        }
        //参数验签
        this.secret = config.getString("secret");//从配置中获取
        this.merchId = config.getString("merchId");//从配置中获取
        this.queryOrderUrl = config.getString("queryOrderUrl");//从配置中获取

        String order_amount = infoMap.get("payAmt");//单位：分
        if (StringUtils.isBlank(order_amount)) {
            logger.info("KPAYNotify获取实际支付金额为空!");
            return ret__failed;
        }
        double realAmount = Double.parseDouble(order_amount)/100;
        String order_no = infoMap.get("spbillno");// 平台订单号
        String trade_no = infoMap.get("transactionId");// 第三方订单号
        String trade_status = infoMap.get("result");
        String t_trade_status = trade_status;// 表示成功状态

        /**订单查询*/
        if (!serchOrder(order_no, trade_no)) {
            logger.info("[KPAY]kpay支付回调查询订单{}失败", order_no);
            return ret__failed;
        }
        /**回调验签*/
        boolean verifyRequest = verifyCallback(infoMap);

        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);
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
        processNotifyVO.setPayment("KPAY");
        processNotifyVO.setConfig(config);
        return super.processSuccessNotify(processNotifyVO, verifyRequest);
    }
    public String getReqXml(Map<String, String> data) throws Exception{
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        Set<String> keySet = data.keySet();
        for (String key : keySet) {
            sb.append("<"+key+">").append(data.get(key)).append("</"+key+">");
        }
        sb.append("</xml>");
        return sb.toString();
    }
}
