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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author Vicky
 * @version 1.2.0
 * @ClassName BAIPayServiceImpl
 * @Description 百付支付  渠道 支付宝 微信
 * @Date 2019/5/14 17 20
 **/
public class BAIPayServiceImpl extends PayAbstractBaseService implements PayService {

    private static final Logger logger = LoggerFactory.getLogger(BAIPayServiceImpl.class);

    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true

    public BAIPayServiceImpl() {
    }

    public BAIPayServiceImpl(Map<String, String> data) {
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
        logger.info("[BAI]百付支付扫码支付回调请求参数：" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("BAINotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("trade_id");//第三方订单号，流水号
        String order_no = dataMap.get("out_trade_no");//支付订单号

        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[BAI]百付支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        String trade_status = dataMap.get("trade_state");  //第三方支付状态，1 支付成功
        String t_trade_status = "1";//第三方成功状态
        String amount =null;//实际支付金额,以分为单位
        //订单查询
        try{
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("app_id", merchId);
            queryMap.put("out_trade_no", order_no);

            logger.info("[BAI]百付支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));
            String ponse = HttpUtils.toPostForm(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[BAI]百付支付回调查询订单发起HTTP请求无响应");
                return ret__failed;
            }
            logger.info("[BAI]百付支付扫码支付回调查询订单{}响应信息：{}", order_no, JSONObject.fromObject(ponse));

            JSONObject jb = JSONObject.fromObject(ponse);

            if(jb.containsKey("code") && "0".equals(jb.getString("code"))){

                if(!"1".equals(jb.getString("trade_state"))){
                    logger.info("[BAI]百付支付扫码支付回调查询订单,订单支付状态为:{}",jb.getString("trade_state"));
                    return ret__failed;
                }
            }else {
                logger.info("[BAI]百付支付扫码支付回调查询订单,请求状态为:{}",jb.getString("code"));
                return ret__failed;
            }
            //用户实际支付金额，以分为单位
            amount = jb.getString("total_amount");
        }catch (Exception e){
            e.getStackTrace();
            logger.info("[BAI]百付支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
            return ret__failed;
        }

        //写入数据库
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setRealAmount(Double.parseDouble(amount)/100);//以分为单位
        processNotifyVO.setIp(ip);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setRet__success(ret__success);
        processNotifyVO.setInfoMap(JSONObject.fromObject(dataMap).toString());
        processNotifyVO.setT_trade_status(t_trade_status);
        processNotifyVO.setConfig(config);
        processNotifyVO.setPayment("BAI");

        //回调验签
//        if ("fail".equals(callback(dataMap))) {
//            verifySuccess = false;
//            logger.info("[BAI]百付支付回调验签失败");
//            return ret__failed;
//        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        return null;
    }

    /**
     * 扫码支付
     * @param payEntity
     * @return
     */
    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[BAI]百付支付扫码支付开始===============START========================");
        try{
            Map<String, String> dataMap = sealRequest(payEntity);
            String responseData = HttpUtils.toPostForm(dataMap, payUrl);
//            return PayResponse.sm_form(payEntity, responseData, "下单成功");
            logger.info("[BAI]百付支付扫码支付响应信息：{}", responseData);
            if(StringUtils.isBlank(responseData)){
                logger.info("[BAI]百付支付发起HTTP请求无响应");
                return PayResponse.error("[BAI]百付支付扫码支付发起HTTP请求无响应");
            }

            JSONObject json = JSONObject.fromObject(responseData);
            if(json.containsKey("code") && "0".equals(json.getString("code"))){
                return PayResponse.sm_qrcode(payEntity, json.getString("qrcode_url"), "下单成功");
            }
            return PayResponse.error("下单失败");

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[BAI]百付支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[BAI]百付支付扫码支付异常");
        }
    }

    /**
     * 回调验签
     * @param data
     * @return
     */
    @Override
    public String callback(Map<String, String> data) {

        String sign = generatorSign(data);
        String sourceSign = data.remove("sign");
        logger.info("[BAI]百付支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);

        if(sign.equals(sourceSign)){
            return "success";
        }
        return "fail";
    }

    /**
     * 组装参数
     * @param payEntity
     * @return
     */
    private Map<String, String> sealRequest(PayEntity payEntity){
        Map<String, String> dataMap = new HashMap<>();
        String amount = new DecimalFormat("##").format(payEntity.getAmount()*100);
        Date data = new Date();

        dataMap.put("app_id", merchId);//机构id：,用于识别请求平台的机构商应用，同时用于签名
        dataMap.put("out_trade_no", payEntity.getOrderNo());//接入方的本地订单号，接入方请保证这个订单号的唯一性如果订单号不唯一，会影响支付查询结果
        dataMap.put("total_amount", amount);//付款金额单位为【分】
        dataMap.put("timestamp", String.valueOf(data.getTime()));//时间戳，值为当前时间距离19700101的毫秒数
        dataMap.put("sign",generatorSign(dataMap));//签名，详见请求签名规则
        dataMap.put("notice_url", notifyUrl);//在用户支付成功后服务器主动通知商户服务器里指定的页面http/https路径
       // dataMap.put("pay_type", payEntity.getPayCode());

        logger.info("[BAI]百付支付扫码支付请求参数：{}", JSONObject.fromObject(dataMap));
        return dataMap;
    }

    /**
     * 生成签名
     * @param data
     * @return
     */
    private String generatorSign(Map<String, String> data){

       try{
            Map<String, String> treeMap = new TreeMap<>(data);
            StringBuffer sb = new StringBuffer();
            Iterator<String> iterator = treeMap.keySet().iterator();
            while (iterator.hasNext()){
                String key = iterator.next();
                String val = treeMap.get(key);
                if("sign".equals(key) ){
                    continue;
                }
                sb.append(key).append("=").append(val).append("&");
            }
            sb.replace(sb.length()-1,sb.length(),"");
            //sb.append("key=").append(secret);

            String strString = sb.toString();
            logger.info("[BAI]百付支付扫码支付生成待签名串：{}",strString);
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString()).toLowerCase();

            logger.info("[BAI]百付支付扫码支付生成签名串：{}",sign);
            return sign;
        }catch (Exception e){
            e.getStackTrace();
            logger.info("[BAI]百付支付扫码支付生成签名异常：{}",e.getMessage());
            return "[BAI]百付支付生成签名异常";
        }
    }
}
