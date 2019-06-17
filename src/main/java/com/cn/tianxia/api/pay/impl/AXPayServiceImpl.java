package com.cn.tianxia.api.pay.impl;

import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.utils.JSONUtils;
import com.cn.tianxia.api.utils.ax.AesUtil;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;
import com.cn.tianxia.api.utils.pay.MapUtils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.vo.ProcessNotifyVO;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Vicky
 * @version 1.2.0
 * @ClassName AXPayServiceImpl
 * @Description 安兴支付  渠道 支付宝
 * @Date 2019/5/17 9 30
 **/
public class AXPayServiceImpl extends PayAbstractBaseService implements PayService {

    private static final Logger logger = LoggerFactory.getLogger(AXPayServiceImpl.class);
    private static String ret__success = "{\"ErrCode\":0,\"Message\":\"成功\"}";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;
    private boolean verifySuccess = true;//回调验签默认状态为true


    public AXPayServiceImpl() {
    }

    public AXPayServiceImpl(Map<String, String> data) {
        if (MapUtils.isNotEmpty(data)) {
            if (data.containsKey("merchId")) {
                this.merchId = data.get("merchId");
            }
            if (data.containsKey("payUrl")) {
                this.payUrl = data.get("payUrl");
            }
            if (data.containsKey("notifyUrl")) {
                this.notifyUrl = data.get("notifyUrl");
            }
            if (data.containsKey("secret")) {
                this.secret = data.get("secret");
            }
            if (data.containsKey("queryOrderUrl")) {
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
        }
    }

    /**
     * 回调
     *
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
        logger.info("[AX]安兴支付扫码支付回调请求参数：" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("AXNotify获取回调请求参数为空");
            return ret__failed;
        }
        //对数据体进行解密
        String data = AesUtil.decrypt(secret, dataMap.get("Data"));
        logger.info("[AX]安兴支付扫码支付回调请求参数,data数据体解密后：" + JSONObject.fromObject(data));
        JSONObject resJson = JSONObject.fromObject(data);

        String trade_no = resJson.getString("Id");//第三方订单号，流水号
        String order_no = resJson.getString("MerchantOrderCode");//支付订单号
        String amount = resJson.getString("ActualAmount");//实际支付金额,以元为单位
        String trade_status = resJson.getString("PayStatus");  //第三方支付状态，1 支付成功
        String t_trade_status = trade_status;//第三方成功状态

        if (!getOrderStatus(order_no, trade_no)) {
            logger.info("[AX]安兴支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }

        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);
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
        processNotifyVO.setPayment("AX");

        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[AX]安兴支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    /**
     * 功能描述:查询订单状态
     *
     * @param orderNo 订单号
     * @return: boolean
     **/
    private boolean getOrderStatus(String orderNo, String tradeNo) {
        //订单查询
        try {
            JSONObject reqJson = new JSONObject();
            reqJson.put("SysOrderId", tradeNo);
            reqJson.put("MerchantOrderCode", orderNo);

            reqJson = encryptData(reqJson);
            logger.info("[AX]安兴支付扫码支付回调查询订单{}加密后请求参数：{}", orderNo, reqJson);
            String resStr = HttpUtils.toPostJsonStr(reqJson, queryOrderUrl);

            if (StringUtils.isBlank(resStr)) {
                logger.info("[AX]安兴支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            JSONObject respJson = JSONObject.fromObject(resStr);
            logger.info("[AX]安兴支付扫码支付回调查询订单{}响应信息：{}", orderNo, respJson);
            if (JSONUtils.compare(respJson, "ErrCode", "0")) {
                String decrypt = AesUtil.decrypt(secret, respJson.getString("Data"));
                logger.info("[AX]安兴支付扫码支付回调查询订单响应Data参数解密信息：{}", decrypt);
                respJson = JSONObject.fromObject(decrypt);
                if (JSONUtils.compare(respJson, "PayStatus", "3")) {
                    logger.info("[AX]安兴支付扫码支付回调订单查询成功,订单" + orderNo + "已支付。");
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.getStackTrace();
            logger.info("[AX]安兴支付扫码支付回调查询订单{}异常{}：", orderNo, e.getMessage());
            return false;
        }
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        return null;
    }

    /**
     * 扫码支付
     *
     * @param payEntity
     * @return
     */
    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[AX]安兴支付扫码支付开始===============START========================");
        try {
            JSONObject jsonData = sealRequest(payEntity);
            logger.info("[AX]安兴支付扫码支付加密前请求参数：{}", jsonData);
            //最后请求时的参数
            JSONObject reqJson = encryptData(jsonData);
            logger.info("[AX]安兴支付扫码支付加密后请求参数：{}", reqJson);
            String responseData = HttpUtils.toPostJsonStr(reqJson, payUrl);
            logger.info("[AX]安兴支付扫码支付响应信息：{}", responseData);
            if (StringUtils.isBlank(responseData)) {
                logger.info("[AX]安兴支付发起HTTP请求无响应");
                return PayResponse.error("[AX]安兴支付扫码支付发起HTTP请求无响应");
            }
            JSONObject resJson = JSONObject.fromObject(responseData);
            if (resJson.containsKey("ErrCode") && "0".equals(resJson.getString("ErrCode"))) {
                String resStr = AesUtil.decrypt(secret, resJson.getString("Data"));
                resJson = JSONObject.fromObject(resStr);
                logger.info("[AX]安兴扫码支付解码后响应信息：{}", resJson);
                return PayResponse.sm_link(payEntity, resJson.getString("PayUrl"), "下单成功");
            }
            return PayResponse.error("[AX]安兴扫码支付失败" + resJson.getString("Message"));

        } catch (Exception e) {
            e.getStackTrace();
            logger.info("[AX]安兴支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[AX]安兴支付扫码支付异常");
        }
    }

    /**
     * 回调验签
     *
     * @param data
     * @return
     */
    @Override
    public String callback(Map<String, String> data) {
        String sign = generatorSign(data);
        String sourceSign = data.remove("Sign");
        logger.info("[AX]安兴支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);

        if (sign.equals(sourceSign)) {
            return "success";
        }
        return "fail";
    }

    /**
     * 组装参数
     *
     * @param payEntity
     * @return
     */
    private JSONObject sealRequest(PayEntity payEntity) {
        JSONObject jsonData = new JSONObject();
        String amount = new DecimalFormat("0.00").format(payEntity.getAmount());
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 4);//随机字符
        jsonData.put("MerchantOrderCode", payEntity.getOrderNo());//商户订单号
        jsonData.put("PayType", payEntity.getPayCode());//支付方式
        jsonData.put("Amount", amount);//订单金额
        jsonData.put("ShouldCollectAmount", amount);//订单应收金额
        jsonData.put("PayCodeNotifyUrl", "");//异步通知商户支付二维码信息的接口地址
        jsonData.put("PayResultNotifyUrl", notifyUrl);//异步通知商户支付结果的接口地址
        jsonData.put("MerchantRemark", nonce);//商户备注码

        logger.info("[AX]安兴支付扫码支付请求参数中Data的参数明细：{}", jsonData);
        return jsonData;
    }

    /**
     * 加密data参数
     *
     * @param
     * @return
     */
    private JSONObject encryptData(JSONObject jsonData) {
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 7);//随机字符
        String time = String.valueOf(System.currentTimeMillis() / 1000);//时间戳

        Map<String, String> map = new HashMap<>();
        String data = AesUtil.encrypt(secret, jsonData.toString());
        map.put("AppId", merchId);//商户ID
        map.put("TimeStamp", time);//时间戳10位
        map.put("NonceStr", nonce);//长度为7字符的随机字符串
        map.put("Data", data);//入参数据对象
        map.put("Sign", generatorSign(map));//
        JSONObject resJson = JSONObject.fromObject(map);
        return resJson;
    }

    /**
     * 生成签名
     *
     * @param dataMap
     * @return
     */
    private String generatorSign(Map<String, String> dataMap) {

        try {
            StringBuffer sb = new StringBuffer();
            sb.append(dataMap.get("Data")).append(secret).append(dataMap.get("AppId"))
                    .append(dataMap.get("NonceStr")).append(dataMap.get("TimeStamp"));

            String signStr = sb.toString();
            logger.info("[AX]安兴支付扫码支付生成待签名串：{}", signStr);
            String sign = MD5Utils.md5toUpCase_32Bit(signStr).toLowerCase();

            logger.info("[AX]安兴支付扫码支付生成签名串：{}", sign);
            return sign;
        } catch (Exception e) {
            e.getStackTrace();
            logger.info("[AX]安兴支付扫码支付生成签名串异常：{}", e.getMessage());
            return "[AX]安兴支付生成签名异常";
        }
    }
}
