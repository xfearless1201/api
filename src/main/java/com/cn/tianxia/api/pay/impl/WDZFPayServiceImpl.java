package com.cn.tianxia.api.pay.impl;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Auther: zed
 * @Date: 2019/1/22 14:06
 * @Description: 万达支付
 */
public class WDZFPayServiceImpl extends PayAbstractBaseService implements PayService {
    //日志
    private static final Logger logger = LoggerFactory.getLogger(WDZFPayServiceImpl.class);

    private String Key;//秘钥
    private String alipayUrl;//支付宝支付地址
    private String wxpayUrl;//微信
    private String notifyUrl;//回调地址
    private String userId;//商户号
    private String queryOrderUrl;

    /**
     * 回调失败响应信息
     */
    private static final String ret__failed = "fail";
    /**
     * 回调成功响应信息
     */
    private static final String ret__success = "success";
    private boolean verifySuccess = true;//回调验签默认状态为true

    public WDZFPayServiceImpl() {
    }

    //构造器,初始化参数
    public WDZFPayServiceImpl(Map<String, String> data) {
        if (MapUtils.isNotEmpty(data)) {
            if (data.containsKey("Key")) {
                this.Key = data.get("Key");
            }
            if (data.containsKey("alipayUrl")) {
                this.alipayUrl = data.get("alipayUrl");
            }
            if (data.containsKey("wxpayUrl")) {
                this.wxpayUrl = data.get("wxpayUrl");
            }
            if (data.containsKey("notifyUrl")) {
                this.notifyUrl = data.get("notifyUrl");
            }
            if (data.containsKey("userId")) {
                this.userId = data.get("userId");
            }
            if (data.containsKey("queryOrderUrl")) {
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
        }
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[WDZF]万达支付扫码支付开始==============START==============");
        try {
            //获取支付请求参数
            Map<String, String> data = sealRequest(payEntity);
            logger.info("[WDZF]万达支付扫码支付请求参数报文:{}", JSONObject.fromObject(data).toString());

            String response = null;
            if("ALIPAY".equalsIgnoreCase(payEntity.getPayCode())){
                response = HttpUtils.toPostForm(data, alipayUrl);
            }else {
                response = HttpUtils.toPostForm(data, wxpayUrl);
            }

            if (StringUtils.isBlank(response)) {
                logger.info("[WDZF]万达支付扫码支付发起HTTP请求无响应结果");
                return PayResponse.error("[WDZF]万达支付扫码支付发起HTTP请求无响应结果");
            }

            //解析响应结果
            JSONObject jsonObject = JSONObject.fromObject(response);
            if (jsonObject.containsKey("code") && "200".equals(jsonObject.getString("code"))) {
                //成功
                JSONObject result = JSONObject.fromObject(jsonObject.get("data"));
                String url = result.getString("payUrl");
                return PayResponse.sm_link(payEntity, url, "下单成功");
            }
            return PayResponse.error("下单失败:" + response);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[WDZF]万达支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[WDZF]万达支付扫码支付异常");
        }
    }

    @Override
    public String callback(Map<String, String> data) {
        logger.info("[WDZF]万达支付回调验签开始==================START==================");
        try {
            String sourceSign = data.remove("sign");
            String sign = generatorSign(data, 2);
            logger.info("[WDZF]万达支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            if(sourceSign.equalsIgnoreCase(sign)){
                return "success";
            }
            return "fail";
        }catch (Exception e){
            e.getStackTrace();
            logger.info("" + e.getMessage());
            return "fail";
        }
    }

    /**
     * @param entity
     * @return
     * @throws Exception
     * @Description 封装支付请求参数
     */
    private Map<String, String> sealRequest(PayEntity entity) throws Exception {
        logger.info("[WDZF]万达支付组装支付请求参数开始==================START==================");
        try {
            Map<String, String> data = new HashMap<>();
            String amount = new DecimalFormat("0.00").format(entity.getAmount());
            data.put("CustomerId", entity.getOrderNo());// 商户订单号，确保应用内唯一
            data.put("Mode", "8");//8:支付宝支付
            data.put("BankCode", entity.getPayCode());//  银行编码 例如,支付宝:ALIPAY  详见 **附录(银行编码)**
            data.put("Money", amount);// 订单金额，单位为元，保留两位小数
            data.put("UserId", userId);// 支付平台分配的商户号merno
            data.put("Message", "top_Up");// 订单备注，在回调通知中将原样返回
            data.put("CallBackUrl", notifyUrl);//  异步通知地址，用于接收支付结果回调。相关规则详见2.2 异步回调通知机制
            data.put("ReturnUrl", entity.getRefererUrl());//  支付返回地址，支付成功时同步跳转到此地址
            data.put("Sign", generatorSign(data,1 ));// 数据签名,详见3.1签名算法
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[WDZF]万达支付组装支付请求参数异常:{}", e.getMessage());
            throw new Exception("[WDZF]万达支付组装支付请求参数异常");
        }
    }

    /**
     * @param data
     * @return
     * @throws Exception
     * @Description 生成签名串
     */
    private String generatorSign(Map<String, String> data, int type) throws Exception {
        logger.info("[WDZF]万达支付生成签名开始===============START======================");
        try {
            //签名规则:
            //直接把请求数据中的所有元素(除sign本身)按照“key值=value值”的格式拼接起来，
            //并且把这些拼接以后的元素以“&”字符再连接起来（把每一项按常规顺序排列[Standard ASCII，不改变类型]），值为空的去除），
            //url之类用urldecode解码。 然后用商户设定的secretkey，执行hmacSha256计算，以Base64转码的结果(大写)为签名串sign。
            StringBuffer sb = new StringBuffer();

            if(1 == type){
                sb.append("BankCode=").append(data.get("BankCode"))
                        .append("&CallBackUrl=").append(data.get("CallBackUrl"))
                        .append("&CustomerId=").append(data.get("CustomerId"))
                        .append("&Message=").append(data.get("Message"))
                        .append("&Mode=").append(data.get("Mode"))
                        .append("&Money=").append(data.get("Money"))
                        .append("&ReturnUrl=").append(data.get("ReturnUrl"))
                        .append("&UserId=").append(data.get("UserId"));
            }else if (2 == type){
                sb.append("CustomerId=").append(data.get("customerId"));
                sb.append("&OrderId=").append(data.get("orderId"));
                sb.append("&Money=").append(data.get("money"));
                sb.append("&Status=").append(data.get("status"));
                sb.append("&Message=").append(data.get("message"));
                sb.append("&Type=").append(data.get("type"));

            }else {
                sb.append("CustomerId=").append(data.get("CustomerId"));
                sb.append("&OrderType=").append(data.get("OrderType"));
                sb.append("&UserId=").append(data.get("UserId"));

            }

            sb.append("&Key=").append(Key);
            String signStr = sb.toString();
            logger.info("[WDZF]万达支付生成待签名串:{}", signStr);
            String sign = MD5Utils.md5toUpCase_32Bit(signStr).toLowerCase();
            logger.info("[WDZF]万达支付生成加密签名串：{}", sign.toLowerCase());
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[WDZF]万达支付生成签名异常:{}", e.getMessage());
            throw new Exception("[WDZF]万达支付生成签名异常");
        }
    }

    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        Map<String, String> infoMap = ParamsUtils.getNotifyParams(request);  //获取回调请求参数
        logger.info("[WDZF]万达支付码支付回调请求参数：" + JSONObject.fromObject(infoMap));
        if (org.apache.commons.collections.MapUtils.isEmpty(infoMap)) {
            logger.error("WDZFNotify获取回调请求参数为空");
            return ret__failed;
        }
        //参数验签
        this.Key = config.getString("Key");//从配置中获取
        this.userId = config.getString("userId");//从配置中获取
        this.queryOrderUrl = config.getString("queryOrderUrl");//从配置中获取

        String order_no = infoMap.get("customerId");// 平台订单号
        String trade_no = infoMap.get("orderId");// 第三方流水号
        String trade_status = infoMap.get("status");//PAID表示业务成功
        String t_trade_status = "1";// 1 成功 0 失败
        Double realAmount = Double.valueOf(infoMap.get("money")); //元为单位

        /**订单查询*/
        if (!serchOrder(order_no)) {
            logger.info("[WDZF]万达支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }
        //回调验签
        if ("fail".equals(callback(infoMap))) {
            verifySuccess = false;
            logger.info("WDZF]万达支付回调验签失败");
            return ret__failed;
        }

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
        processNotifyVO.setPayment("WDZF");
        processNotifyVO.setConfig(config);
        return super.processSuccessNotify(processNotifyVO, verifySuccess);
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
            param.put("CustomerId", orderNo);//商户订单号
            param.put("OrderType","1");//固定值 1 ，表示充值订单
            param.put("UserId", userId);// 支付平台分配的商户号merno
            param.put("Sign", generatorSign(param,3));//数据签名,详见3.1签名算法

            logger.info("[WDZF]万达支付扫码支付回调查询订单{}请求参数：{}", orderNo, JSONObject.fromObject(param));
            String resStr = HttpUtils.toPostForm(param, queryOrderUrl);
            logger.info("[WDZF]万达支付扫码支付回调查询订单{}响应信息：{}", orderNo, JSONObject.fromObject(resStr));
            if (StringUtils.isBlank(resStr)) {
                logger.info("[WDZF]万达支付扫码支付回调查询订单发起HTTP请求无响应,订单号{}", orderNo);
                return false;
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if("200".equalsIgnoreCase(resJson.getString("code"))){
                JSONObject json = resJson.getJSONObject("data");
                if("1".equalsIgnoreCase(json.getString("status"))){
                    return true;
                }else {
                    logger.info("[WDZF]万达支付扫码支付回调查询订单{}，支付状态：{}", orderNo,resJson.getString("message"));
                    return false;
                }
            }else {
                logger.info("[WDZF]万达支付扫码支付回调查询订单{}，查询状态：{}", orderNo,resJson.getString("msg"));
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[WDE]万德扫码支付回调查询订单{}异常{}", orderNo, e.getMessage());
            return false;
        }
    }
}
