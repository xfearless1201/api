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
import com.cn.tianxia.api.utils.JSONUtils;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.vo.ProcessNotifyVO;

import net.sf.json.JSONObject;

/**
 *  * @ClassName YXFPayServiceImpl
 *  * @Description TODO(言信支付)
 *  * @Author Bing
 *  * @Date 2019年05月23日 10:25
 *  * @Version 1.0.0
 *  
 **/
public class YXFPayServiceImpl extends PayAbstractBaseService implements PayService {
    private static final Logger logger = LoggerFactory.getLogger(YXFPayServiceImpl.class);
    /**
     * 回调失败响应信息
     */
    private static final String ret__failed = "fail";
    /**
     * 回调成功响应信息
     */
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
    
    private String email;
    private String accessMode;
    

    public YXFPayServiceImpl() {
    }

    public YXFPayServiceImpl(Map<String, String> data) {
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
            if (data.containsKey("queryOrderUrl")) {
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
            if (data.containsKey("email")) {
                this.email = data.get("email");
            }
            if (data.containsKey("accessMode")) {
                this.accessMode = data.get("accessMode");
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
        return null;
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
            Map<String, String> data = sealRequest(payEntity);
            logger.info("[YXF]言信扫码支付请求参数:{}", JSONObject.fromObject(data));
            String resStr = null;
            if("1".equals(accessMode)||"3".equals(accessMode)) {
                resStr = HttpUtils.generatorForm(data, payUrl);
                return PayResponse.sm_form(payEntity, resStr, "下单成功");
            }{
                resStr = HttpUtils.toPostForm(data, payUrl);
            }
            logger.info("[YXF]言信扫码支付响应信息：{}", resStr);
            if (StringUtils.isBlank(resStr)) {
                logger.info("[YXF]言信扫码支付下单失败，无响应结果");
                return PayResponse.error("[YXF]言信扫码支付下单失败，无响应结果");
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if(JSONUtils.compare(resJson, "code", "success")) {
                return PayResponse.sm_link(payEntity, resJson.getString("msg"), "下单成功");
            }
            return PayResponse.error("[YXF]言信扫码支付下单失败："+resJson.getString("msg"));
        } catch (Exception e) {
            e.printStackTrace();
            return PayResponse.error("[YXF]言信扫码支付下单异常" + e.getMessage());
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
    private Map<String, String> sealRequest(PayEntity payEntity) throws Exception {
        String amount = new DecimalFormat("0.00").format(payEntity.getAmount());
        Map<String, String> data = new HashMap<>();
        data.put("service", "online_pay");//接口名称
        data.put("customer_no", merchId);//商户号
        data.put("notify_url", notifyUrl);//通知地址
        data.put("return_url", payEntity.getRefererUrl());//跳转地址
        data.put("charset", "UTF-8");//字符集
        data.put("title", "recharge");//商品名称
        data.put("body", "recharge");//商品描述
        data.put("order_no", payEntity.getOrderNo());//订单号
        data.put("total_fee", amount);//金额 单位：元
        data.put("payment_type", "1");//交易类型
        data.put("paymethod", "directPay");//支付方式
        data.put("defaultbank", payEntity.getPayCode());//网银代码
        data.put("access_mode", accessMode);//接入方式
        data.put("seller_email", email);//
        data.put("sign", generatorSign(data));//签名
        data.put("sign_type", "MD5");//
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
            sb.deleteCharAt(sb.length()-1);
            sb.append(secret);
            String signStr = sb.toString();
            logger.info("[YXF]言信扫码支付生成待签名串:" + signStr);
            String sign = MD5Utils.md5(signStr.getBytes());
            logger.info("[YXF]言信扫码支付生成MD5加密签名串:" + sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[YXF]言信扫码支付生产签名串异常:" + e.getMessage());
            throw new Exception("[YXF]言信扫码支付生成签名串异常!");
        }
    }

    /**
     * 订单查询接口
     *
     * @param orderNo
     * @return
     * @Description (TODO这里用一句话描述这个方法的作用)
     */
    public boolean serchOrder(String orderNo) {
        try {
            Map<String, String> param = new HashMap<>();
            param.put("partner", merchId);//商户号
            param.put("charset", "utf8");//字符集
            param.put("order_no", orderNo);//字符集
            param.put("return_type", "json");//返回类型
            param.put("sign", generatorSign(param));
            param.put("sign_type", "MD5");
            logger.info("[YXF]言信扫码支付回调查询订单{}请求参数：{}", orderNo, JSONObject.fromObject(param));
            String resStr = HttpUtils.toPostForm(param, queryOrderUrl);
            logger.info("[YXF]言信扫码支付回调查询订单{}响应信息：{}", orderNo, JSONObject.fromObject(resStr));
            if (StringUtils.isBlank(resStr)) {
                logger.info("[YXF]言信扫码支付回调查询订单发起HTTP请求无响应,订单号{}", orderNo);
                return false;
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            resJson = resJson.getJSONObject("ebank");
            if (!"T".equals(resJson.getString("is_success"))) {
                return false;
            }
            resJson = resJson.getJSONObject("trade");
            if (!"completed".equalsIgnoreCase(resJson.getString("status"))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YXF]言信扫码支付回调查询订单{}异常{}", orderNo, e.getMessage());
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
            data.remove("sign_type");
            String sourceSign = data.remove("sign");
            String sign = generatorSign(data);
            logger.info("[YXF]言信扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            return sourceSign.equalsIgnoreCase(sign);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YXF]言信扫码支付回调生成签名串异常{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        Map<String, String> infoMap = ParamsUtils.getNotifyParams(request);  //获取回调请求参数
        logger.info("[YXF]言信扫码支付回调请求参数：" + JSONObject.fromObject(infoMap));
        if (MapUtils.isEmpty(infoMap)) {
            logger.error("YXFNotify获取回调请求参数为空");
            return ret__failed;
        }
        //参数验签
        this.secret = config.getString("secret");//从配置中获取
        this.merchId = config.getString("merchId");//从配置中获取
        this.queryOrderUrl = config.getString("queryOrderUrl");//从配置中获取

        String order_amount = infoMap.get("total_fee");//单位：元
        if (StringUtils.isBlank(order_amount)) {
            logger.info("YXFNotify获取实际支付金额为空!");
            return ret__failed;
        }
        double realAmount = Double.parseDouble(order_amount);
        String order_no = infoMap.get("order_no");// 平台订单号
        String trade_no = infoMap.get("trade_no");// 第三方订单号
        String trade_status = infoMap.get("trade_status");
        String t_trade_status = "TRADE_FINISHED";// 表示成功状态

        /**订单查询*/
        if (!serchOrder(order_no)) {
            logger.info("[YXF]言信扫码支付回调查询订单{}失败", order_no);
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
        processNotifyVO.setPayment("YXF");
        processNotifyVO.setConfig(config);
        return super.processSuccessNotify(processNotifyVO, verifyRequest);
    }
}
