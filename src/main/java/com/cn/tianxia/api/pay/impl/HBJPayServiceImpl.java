package com.cn.tianxia.api.pay.impl;


import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.vo.ProcessNotifyVO;
import net.sf.json.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  * @ClassName AXFPayServiceImpl
 *  * @Description TODO(海贝金付支付)
 *  * @Author Vicky
 *  * @Date 2019年06月12日 13:18
 *  * @Version 1.0.0
 *  
 **/
public class HBJPayServiceImpl extends PayAbstractBaseService implements PayService {
    private static final Logger logger = LoggerFactory.getLogger(HBJPayServiceImpl.class);
    /**
     * 回调失败响应信息
     */
    private static final String ret__failed = "fail";
    /**
     * 回调成功响应信息
     */
    private static final String ret__success = "OK";
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

    public HBJPayServiceImpl() {
    }
    public HBJPayServiceImpl(Map<String, String> data) {
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
            logger.info("[HBJ]海贝金付支付扫码支付请求参数:{}", JSONObject.fromObject(data));
            String resStr = HttpUtils.generatorForm(data, payUrl);
            return PayResponse.sm_form(payEntity, resStr, "下单成功");

        } catch (Exception e) {
            e.printStackTrace();
            return PayResponse.error("[HBJ]海贝金付支付扫码支付下单异常" + e.getMessage());
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
        String toDate = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date());
        String amount = new  DecimalFormat("0.00").format(payEntity.getAmount());
        Map<String,String> dataMap = new HashMap<>();

        dataMap.put("pay_memberid", merchId);//商户编号商户号是是平台分配商户号
        dataMap.put("pay_orderid", payEntity.getOrderNo());//商户编号订单号是是上送订单号唯一, 字符长度20
        dataMap.put("pay_applydate", toDate);//商户编号提交时间是是时间格式：2016-12-26 18:18:18
        dataMap.put("pay_bankcode", payEntity.getPayCode());//商户编号银行编码是是参考后续说明
        dataMap.put("pay_notifyurl", notifyUrl);//商户编号服务端通知是是服务端返回地址.（POST返回数据）
        dataMap.put("pay_callbackurl", payEntity.getRefererUrl());//商户编号页面跳转通知是是页面跳转返回地址（POST返回数据）
        dataMap.put("pay_amount", amount);//商户编号订单金额是是商品金额
        dataMap.put("pay_md5sign",generatorSign(dataMap));//商户编号MD5签名是否请看MD5签名字段格式
        dataMap.put("pay_productname", "recharge");
        return dataMap;
    }

    /**
     * @param data
     * @return
     * @throws Exception
     * @Description 生产签名串
     */
    private String generatorSign(Map<String, String> data) throws Exception {
        //1.stringSignTemp="pay_amount=pay_amount&pay_applydate=pay_applydate&pay_bankcode=pay_bankcode&
        // pay_callbackurl=pay_callbackurl&pay_memberid=pay_memberid&pay_notifyurl=pay_notifyurl&
        // pay_orderid=pay_orderid&key=key"
        Map<String, String> treeMap = new TreeMap<>(data);
        StringBuffer sb = new StringBuffer();
        Iterator<String> iterator = treeMap.keySet().iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            String val = treeMap.get(key);
            if(StringUtils.isBlank(val) || "sign".equals(key) || "pay_md5sign".equals(key)
                    || "attach".equalsIgnoreCase(key)){
                continue;
            }
            sb.append(key).append("=").append(val).append("&");
        }
        sb.append("key=").append(secret);
        String signStr = sb.toString();
        logger.info("[HBJ]海贝金付支付扫码支付生成待签名串:" + signStr);
        String sign = MD5Utils.md5toUpCase_32Bit(signStr);
        logger.info("[HBJ]海贝金付支付扫码支付生成MD5加密签名串:" + sign);
        return sign;
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
            param.put("pay_memberid", merchId);//商户号
            param.put("pay_orderid", orderNo);//商户订单号
            param.put("pay_md5sign", generatorSign(param));

            logger.info("[HBJ]海贝金付支付扫码支付回调查询订单{}请求参数：{}", orderNo, JSONObject.fromObject(param));
            String resStr = HttpUtils.toPostForm(param, queryOrderUrl);
            logger.info("[HBJ]海贝金付支付扫码支付回调查询订单{}响应信息：{}", orderNo, JSONObject.fromObject(resStr));
            if (StringUtils.isBlank(resStr)) {
                logger.info("[HBJ]海贝金付支付扫码支付回调查询订单发起HTTP请求无响应,订单号{}", orderNo);
                return false;
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if (!"00".equalsIgnoreCase(resJson.getString("returncode"))) {
                return false;
            }
            if (!"SUCCESS".equalsIgnoreCase(resJson.getString("trade_state"))) {
                logger.info("[HBJ]海贝金付支付扫码支付回调查询订单{}订单支付状态：{}", orderNo, resJson.getString("trade_state"));
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[HBJ]海贝金付支付扫码支付回调查询订单{}异常{}", orderNo, e.getMessage());
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
            String sign = generatorSign(data);
            String sourceSign = data.remove("sign");
            logger.info("[HBJ]海贝金付支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            return sourceSign.equalsIgnoreCase(sign);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[HBJ]海贝金付支付扫码支付回调生成签名串异常{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        Map<String, String> infoMap = ParamsUtils.getNotifyParams(request);  //获取回调请求参数
        logger.info("[HBJ]海贝金付支付扫码支付回调请求参数：" + JSONObject.fromObject(infoMap));
        if (MapUtils.isEmpty(infoMap)) {
            logger.error("HBJFNotify获取回调请求参数为空");
            return ret__failed;
        }
        //参数验签
        this.secret = config.getString("secret");//从配置中获取
        this.merchId = config.getString("merchId");//从配置中获取
        this.queryOrderUrl = config.getString("queryOrderUrl");//从配置中获取

        String order_amount = infoMap.get("amount");//单位：元
        if (StringUtils.isBlank(order_amount)) {
            logger.info("HBJNotify获取实际支付金额为空!");
            return ret__failed;
        }
        double realAmount = Double.parseDouble(order_amount);
        String trade_no = infoMap.get("transaction_id");//第三方订单号，流水号
        String order_no = infoMap.get("orderid");//支付订单号
        String trade_status = infoMap.get("returncode");
        String t_trade_status = "00";// 表示成功状态

        /**订单查询*/
        if (!serchOrder(order_no)) {
            logger.info("[HBJ]海贝金付支付扫码支付回调查询订单{}失败", order_no);
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
        processNotifyVO.setPayment("HBJ");
        processNotifyVO.setConfig(config);
        return super.processSuccessNotify(processNotifyVO, verifyRequest);
    }
}
