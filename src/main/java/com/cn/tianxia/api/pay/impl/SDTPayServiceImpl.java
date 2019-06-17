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
 *  * @ClassName SDTPayServiceImpl
 *  * @Description TODO(商点通支付)
 *  * @Author Vicky
 *  * @Date 2019年06月12日 17:18
 *  * @Version 1.0.0
 *  
 **/
public class SDTPayServiceImpl extends PayAbstractBaseService implements PayService {
    private static final Logger logger = LoggerFactory.getLogger(SDTPayServiceImpl.class);
    /**
     * 回调失败响应信息
     */
    private static final String ret__failed = "fail";
    /**
     * 回调成功响应信息
     */
    private static final String ret__success = "SUCCESS";
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

    private String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

    public SDTPayServiceImpl() {
    }
    public SDTPayServiceImpl(Map<String, String> data) {
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
            logger.info("[SDT]商点通支付扫码支付请求参数:{}", JSONObject.fromObject(data));
            String resStr = HttpUtils.toPostForm(data, payUrl);
            logger.info("[SDT]商点通支付扫码支付响应信息：{}", resStr);
            JSONObject json = JSONObject.fromObject(resStr);

            if(json.containsKey("Code") && "0".equals(json.getString("Code"))){
                return PayResponse.sm_link(payEntity, json.getString("Url"), "下单成功");
            }
            return PayResponse.error("下单失败："+json.getString("Message"));

        } catch (Exception e) {
            e.printStackTrace();
            return PayResponse.error("[SDT]商点通支付扫码支付下单异常" + e.getMessage());
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
        String amount = new  DecimalFormat("0.00").format(payEntity.getAmount());
        Map<String,String> dataMap = new HashMap<>();
        dataMap.put("Amount", amount);//支付金额单位元
        dataMap.put("ChannelPlatform", payEntity.getPayCode());//支付网关类型10:支付宝红包
        dataMap.put("Ip", payEntity.getIp());//玩家IP地址请传递真实有效的玩家IP，多次重复或虚假IP会触发风控，影响分配通道
        dataMap.put("MerchantId", merchId);//商户ID见开户文档
        dataMap.put("MerchantUniqueOrderId", payEntity.getOrderNo());//商户请求唯一ID每次提交不可重复，建议使用guid、uuid或时间戳加随机数，通知时会原样返回，最大长度32
        dataMap.put("NotifyUrl", notifyUrl);//支付结果通知地址支付完成后服务器会向此地址发送结果
        dataMap.put("ReturnUrl", "");//支付完成后跳转地址用户支付成功后同步跳转回的地址，可留空，目前暂不跳转
        dataMap.put("Timestamp", time);//请求时间戳格式为 yyyyMMddHHmmss，例如 2019 年 1 月 2 日 3 时 4 分 5 秒则应传递：20190102030405
        dataMap.put("Sign",generatorSign(dataMap));//签名32位小写

        return dataMap;
    }

    /**
     * @param data
     * @return
     * @throws Exception
     * @Description 生产签名串
     */
    private String generatorSign(Map<String, String> data) throws Exception {

        Map<String, String> treeMap = new TreeMap<>(data);
        StringBuffer sb = new StringBuffer();
        Iterator<String> iterator = treeMap.keySet().iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            String val = treeMap.get(key);
            if("Sign".equalsIgnoreCase(key)){
                continue;
            }
            sb.append(key).append("=").append(val).append("&");
        }
        sb.replace(sb.length()-1,sb.length(),secret);
        String signStr = sb.toString();
        logger.info("[SDT]商点通支付扫码支付生成待签名串:" + signStr);
        String sign = MD5Utils.md5toUpCase_32Bit(signStr).toLowerCase();
        logger.info("[SDT]商点通支付扫码支付生成MD5加密签名串:" + sign);
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
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("MerchantId", merchId);//商户ID	见开户文档
            dataMap.put("MerchantUniqueOrderId", orderNo);//商户请求唯一ID	您创建支付订单时提交的订单号
            dataMap.put("Timestamp", time);//请求时间戳	格式为 yyyyMMddHHmmss，例如 2019 年 1 月 2 日 3 时 4 分 5 秒则应传递：20190102030405
            dataMap.put("Sign",generatorSign(dataMap));//签名	32位小写

            logger.info("[SDT]商点通支付扫码支付回调查询订单{}请求参数：{}", orderNo, JSONObject.fromObject(dataMap));
            String resStr = HttpUtils.toPostForm(dataMap, queryOrderUrl);
            logger.info("[SDT]商点通支付扫码支付回调查询订单{}响应信息：{}", orderNo, JSONObject.fromObject(resStr));
            if (StringUtils.isBlank(resStr)) {
                logger.info("[SDT]商点通支付扫码支付回调查询订单发起HTTP请求无响应,订单号{}", orderNo);
                return false;
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if (!"0".equalsIgnoreCase(resJson.getString("Code"))) {
                return false;
            }
            //0为尚未支付，100为支付成功，-90为支付失败
            if (!"100".equalsIgnoreCase(resJson.getString("PayOrderStatus"))) {
                logger.info("[SDT]商点通支付扫码支付回调查询订单"+orderNo+",订单支付状态："+resJson.getString("PayOrderStatus")+",massage:"+ resJson.getString("Message"));
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
            String sourceSign = data.remove("Sign");
            String sign = generatorSign(data);
            logger.info("[SDT]商点通支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            return sourceSign.equalsIgnoreCase(sign);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[SDT]商点通支付扫码支付回调生成签名串异常{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        Map<String, String> infoMap = ParamsUtils.getNotifyParams(request);  //获取回调请求参数
        logger.info("[SDT]商点通支付扫码支付回调请求参数：" + JSONObject.fromObject(infoMap));
        if (MapUtils.isEmpty(infoMap)) {
            logger.error("SDTFNotify获取回调请求参数为空");
            return ret__failed;
        }
        //参数验签
        this.secret = config.getString("secret");//从配置中获取
        this.merchId = config.getString("merchId");//从配置中获取
        this.queryOrderUrl = config.getString("queryOrderUrl");//从配置中获取

        String order_amount = infoMap.get("Amount");//单位：元
        if (StringUtils.isBlank(order_amount)) {
            logger.info("SDTNotify获取实际支付金额为空!");
            return ret__failed;
        }
        double realAmount = Double.parseDouble(order_amount);
        String trade_no = infoMap.get("MerchantUniqueOrderId");//第三方订单号，流水号
        String order_no = infoMap.get("MerchantUniqueOrderId");//支付订单号
        String trade_status = "1";
        String t_trade_status = "1";// 表示成功状态

        /**订单查询*/
        if (!serchOrder(order_no)) {
            logger.info("[SDT]商点通支付扫码支付回调查询订单{}失败", order_no);
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
        processNotifyVO.setPayment("SDT");
        processNotifyVO.setConfig(config);
        return super.processSuccessNotify(processNotifyVO, verifyRequest);
    }
}
