package com.cn.tianxia.api.pay.impl;

import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.dczf.DCZFUtil;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;

import com.cn.tianxia.api.utils.pay.MapUtils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.vo.ProcessNotifyVO;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author Vicky
 * @version 1.2.0
 * @ClassName DCZFPayServiceImpl
 * @Description 东川支付  渠道：微信扫码、微信H5、支付宝扫码、支付宝H5
 * @Date 2019/4/8 10 32
 *
 * 2019/5/23 9:30  重新对接
 **/
public class DCZFPayServiceImpl extends PayAbstractBaseService implements PayService {
    private static final Logger logger = LoggerFactory.getLogger(DCZFPayServiceImpl.class);

    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String queryOrderUrl;//订单查询地址
    private String secret;//密钥
    private String bank;//网银支付 时的渠道编码
    private String key;//私钥
    private String backkey;//平台公钥

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true

    private String nonce = UUID.randomUUID().toString().substring(0, 8);//随机字符

    public DCZFPayServiceImpl() {
    }

    public DCZFPayServiceImpl(Map<String, String> data) {
        if(MapUtils.isNotEmpty(data)){
            if(data.containsKey("merchId")){
                this.merchId = data.get("merchId");
            }
            if(data.containsKey("payUrl")){
                this.payUrl = data.get("payUrl");
            }
            if(data.containsKey("notifyUrl")){
                this.notifyUrl = data.get("notifyUrl");
            }
            if(data.containsKey("queryOrderUrl")){
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
            if(data.containsKey("secret")){
                this.secret = data.get("secret");
            }
            if(data.containsKey("bank")){
                this.bank = data.get("bank");
            }
            if(data.containsKey("key")){
                this.key = data.get("key");
            }
            if(data.containsKey("backkey")){
                this.backkey = data.get("backkey");
            }
        }
    }

    /**
     * 回调
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
        this.bank = config.getString("bank");
        this.key = config.getString("key");
        this.backkey = config.getString("backkey");

        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);
        logger.info("[DCZF]东川支付扫码支付回调请求参数：{}",JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("DCZFNotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("sysOrderId");//第三方订单号，流水号
        String order_no = dataMap.get("orderId");//支付订单号
        String amount = dataMap.get("orderAmt");//实际支付金额,以分为单位
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[DCZF]东川支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        String trade_status = dataMap.get("status");  //第三方支付状态，1 支付成功
        String t_trade_status = "1";//第三方成功状态

        //订单查询
        try{
            Map<String,String> queryMap = new HashMap<>();
            queryMap.put("merId",  merchId);//商户号
            queryMap.put("orderId",  order_no);//商户订单号
            queryMap.put("nonceStr",  nonce);//随机字符
            queryMap.put("sign",  generatorSign(queryMap, 1));

            logger.info("[DCZF]东川支付扫码支付回调查询订单{}请求参数：{}", order_no,JSONObject.fromObject(queryMap));
            String queryData = HttpUtils.toPostForm(queryMap, queryOrderUrl);

            logger.info("[DCZF]东川支付扫码支付回调查询订单{}响应信息：{}", order_no, queryData);
            if(StringUtils.isBlank(queryData)){
                return ret__failed;
            }

            JSONObject jb =JSONObject.fromObject(queryData);
            if(jb.containsKey("code") && "1".equals(jb.getString("code"))){

                JSONObject json = jb.getJSONObject("data");
                if("0".equals(json.getString("status"))){
                    logger.info("[DCZF]东川支付扫码支付回调查询订单,支付状态为:{}", json.getString("status"));
                    return ret__failed;
                }
            }else {
                logger.info("[DCZF]东川支付扫码支付回调查询订单,请求状态为:{}", jb.getString("code"));
                return ret__failed;
            }

        }catch (Exception e){
            e.printStackTrace();
            return ret__failed;
        }

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
        processNotifyVO.setPayment("DCZF");

        //回调验签
        Boolean verify = verifyCallback(dataMap);

        return processSuccessNotify(processNotifyVO, verify);
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        logger.info("[DCZF]东川支付网银支付开始=================start===========================");
        try{

            Map<String, String> dataMap = sealRequest(payEntity, 1);

            String responseData = HttpUtils.toPostForm(dataMap, payUrl);

            if(StringUtils.isBlank(responseData)){
                logger.info("[DCZF]东川支付网银发起HTTP请求无响应");
                return  PayResponse.error("[DCZF]东川支付网银支付发起HTTP请求无响应");
            }

            logger.info("[DCZF]东川支付网银发起HTTP请求返回参数：" + JSONObject.fromObject(responseData));
            return  PayResponse.wy_link(responseData);

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[DCZF]东川支付网银支付异常:{}", e.getMessage());
            return PayResponse.error("网银支付异常:  " + e.getMessage());
        }
    }

    /**
     * 扫码支付
     * @param payEntity
     * @return
     */
    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[DCZF]东川支付扫码支付开始=================start===========================");
        try{
            Map<String, String> dataMap = sealRequest(payEntity, 2);

            String responseData = HttpUtils.toPostForm(dataMap, payUrl);

            if(StringUtils.isBlank(responseData)){
                logger.info("[DCZF]东川支付发起HTTP请求无响应");
                return  PayResponse.error("[DCZF]东川支付扫码支付发起HTTP请求无响应");
            }
            logger.info("[DCZF]东川支付发起HTTP请求返回参数：" + JSONObject.fromObject(responseData));
            JSONObject jb = JSONObject.fromObject(responseData);

            if(jb.containsKey("code") && "1".equals(jb.getString("code"))){

                JSONObject json = jb.getJSONObject("data");
                return  PayResponse.sm_link(payEntity, json.getString("payurl"),"下单成功");
            }
            return  PayResponse.error("下单失败");

        }catch (Exception e){
            e.printStackTrace();
            logger.info("[DCZF]东川支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("扫码支付异常:  " + e.getMessage());
        }
    }

    /**
     * 回调验签
     * @param data
     * @return
     */
    @Override
    public String callback(Map<String, String> data) {
        return null;
    }

    /**
     * 东川支付回调验签
     * @param data
     * @return
     */
    private boolean verifyCallback(Map<String, String> data) {

        //获取回调通知原签名串
        String sourceSign = data.get("sign");
        //生成验签签名串
        String sign = null;
        try {
            sign = generatorSign(data,2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("[DCZF]东川支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
        return DCZFUtil.buildRSAverifyByPublicKey(sign,backkey,sourceSign);
    }

    private Map<String, String> sealRequest(PayEntity payEntity, int type ) throws Exception{
        Map<String, String> dataMap = new HashMap<>();

        String amount = new DecimalFormat("0.00").format(payEntity.getAmount());

        dataMap.put("merId", merchId);//商户号是您在我方平台的商户号
        dataMap.put("orderId", payEntity.getOrderNo());//订单号是订单号，只允许英文和数字
        dataMap.put("orderAmt", amount);//订单金额是订单金额,单位元保留两位小数
        if(1 == type ){//网银
            dataMap.put("channel", bank);//通道代码是通道列表请查看商户后台，或联系商务
            dataMap.put("bankcode",  payEntity.getPayCode());//银行代码 否 用于网银直连模式，请求的银行编号请联系商务，仅网银接口可用。
        }else {
            dataMap.put("channel", payEntity.getPayCode());//通道代码是通道列表请查看商户后台，或联系商务
        }
        dataMap.put("desc", "TOP-UP");//描述是简单描述，只能是汉子、字母数字
        dataMap.put("attch", "recharge");//自定义数据否商户自定义数据，在通知的时候会原样返回
        dataMap.put("smstyle","1");//扫码模式 ,扫码模式，默认0为返回二维码图片1为返回扫码网页地址
        dataMap.put("userId", payEntity.getuId());//用户id ylkj模式必填，标识快捷支付的会员id
        dataMap.put("ip", payEntity.getIp());//ip地址是支付终端的ip地址【风控参数，请务必填写真实ip】
        dataMap.put("notifyUrl", notifyUrl);//异步通知是异步通知地址交易接口本文档使用看云构建-3-
        dataMap.put("returnUrl", payEntity.getRefererUrl());//同步跳转是同步跳转地址
        dataMap.put("nonceStr", nonce);//随机字符串 是 随机字符串，最长不超过32位
        dataMap.put("sign", generatorSign(dataMap, 1));//签名 是 请参考签名算法章节

        logger.info("[DCZF]东川支付扫码支付请求参数：{}", JSONObject.fromObject(dataMap));

        return dataMap;
    }

    /**
     * 生成签名
     * @param data
     * @return
     */

    public String generatorSign(Map<String, String> data, Integer type) throws Exception {
        Map<String, String> sortMap = new TreeMap<>(data);
        StringBuffer sb = new StringBuffer();
        Iterator<String> iterator = sortMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String val = sortMap.get(key);
            if (StringUtils.isBlank(val) || key.equalsIgnoreCase("sign")) {
                continue;
            }
            sb.append(key).append("=").append(val).append("&");
        }
        sb.append("key=").append(secret);
        //生成待签名串
        String singStr = sb.toString();
        logger.info("[DCZF]东川支付扫码支付生成待签名串：{}",singStr);

        //生成加密串
        String sign = MD5Utils.md5toUpCase_32Bit(sb.toString());
        logger.info("[DCZF]东川支付扫码支付生成md5签名串：{}",sign);
        if(1 == type){//支付签名
            String rsaSign = DCZFUtil.buildRSASignByPrivateKey(sign,key);

            logger.info("[DCZF]东川支付扫码支付生成最后rsa签名串：{}",rsaSign);
            return rsaSign;
        }
        return sign;

    }

}
