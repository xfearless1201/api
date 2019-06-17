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
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Vicky
 * @version 1.2.0
 * @ClassName HAPayServiceImpl
 * @Description 汇安支付  渠道 支付宝 微信
 * @Date 2019/5/12 9 30
 **/
public class HAPayServiceImpl extends PayAbstractBaseService implements PayService {

    private static final Logger logger = LoggerFactory.getLogger(HAPayServiceImpl.class);

    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;

    private static String ret__success = "OK";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true

    public HAPayServiceImpl() {
    }

    public HAPayServiceImpl(Map<String, String> data) {
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
        logger.info("[HA]汇安支付扫码支付回调请求参数：" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("HANotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("transaction_id");//第三方订单号，流水号
        String order_no = dataMap.get("orderid");//支付订单号
        String amount = dataMap.get("amount");//实际支付金额,以分为单位
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[HA]汇安支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        String trade_status = dataMap.get("returncode");  //第三方支付状态，1 支付成功
        String t_trade_status = "00";//第三方成功状态

        //订单查询
        try{
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("pay_memberid", merchId);
            queryMap.put("pay_orderid", order_no);
            queryMap.put("pay_md5sign", generatorSign(queryMap));

            logger.info("[HA]汇安支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));
            String ponse = HttpUtils.toPostForm(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[HA]汇安支付回调查询订单发起HTTP请求无响应");
                return ret__failed;
            }
            logger.info("[HA]汇安支付扫码支付回调查询订单{}响应信息：{}", order_no, JSONObject.fromObject(ponse));

            JSONObject jb = JSONObject.fromObject(ponse);
            if(jb.containsKey("returncode") && "00".equals(jb.getString("returncode"))){

                if("NOTPAY".equals(jb.getString("trade_state"))){
                    logger.info("[HA]汇安支付扫码支付回调查询订单,订单支付状态为:{}",jb.getString("trade_state"));
                    return ret__failed;
                }
            }else {
                logger.info("[HA]汇安支付扫码支付回调查询订单,请求状态为:{}",jb.getString("returncode"));
                return ret__failed;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[HA]汇安支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
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
        processNotifyVO.setPayment("HA");

        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[HA]汇安支付回调验签失败");
            return ret__failed;
        }
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
        logger.info("[HA]汇安支付扫码支付开始===============START========================");
        try{
            Map<String, String> dataMap = sealRequest(payEntity);
            String responseData = HttpUtils.generatorForm(dataMap, payUrl);
            logger.info("[HA]汇安支付扫码支付响应信息：{}", responseData);
            if(StringUtils.isBlank(responseData)){
                logger.info("[HA]汇安支付发起HTTP请求无响应");
                return PayResponse.error("[HA]汇安支付扫码支付发起HTTP请求无响应");
            }
            return PayResponse.sm_form(payEntity, responseData, "下单成功");

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[HA]汇安支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[HA]汇安支付扫码支付异常");
        }
    }

    /**
     * 回调验签
     * @param data
     * @return
     */
    @Override
    public String callback(Map<String, String> data) {
        data.remove("attach");
        String sign = generatorSign(data);
        String sourceSign = data.remove("sign");
        logger.info("[HA]汇安支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);

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
        String amount = new DecimalFormat("0.00").format(payEntity.getAmount());
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        dataMap.put("pay_memberid", merchId);//商户号
        dataMap.put("pay_orderid", payEntity.getOrderNo());//订单号
        dataMap.put("pay_amount", amount);//订单金额
        dataMap.put("pay_applydate", time);//提交时间
        dataMap.put("pay_bankcode", payEntity.getPayCode());//银行编码
        dataMap.put("pay_notifyurl", notifyUrl);//服务端通知
        dataMap.put("pay_callbackurl", payEntity.getRefererUrl());//页面跳转通知

        dataMap.put("pay_md5sign",generatorSign(dataMap));//MD5签名
        dataMap.put("pay_attach","TOP-UP");//附加字段
        //dataMap.put("pay_productname","recharge");//商品名称
        //dataMap.put("pay_banktype","");//银行编号


        logger.info("[HA]汇安支付扫码支付请求参数：{}", JSONObject.fromObject(dataMap));
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
                if(StringUtils.isBlank(val) || "sign".equals(key) || "attach".equals(key)){
                    continue;
                }
                sb.append(key).append("=").append(val).append("&");
            }
            sb.append("key=").append(secret);

            String strString = sb.toString();
            logger.info("[HA]汇安支付扫码支付生成待签名串：{}",strString);
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString());

            logger.info("[HA]汇安支付扫码支付生成签名串：{}",sign);
            return sign;
        }catch (Exception e){
            e.getStackTrace();
            logger.info("[HA]汇安支付扫码支付生成签名串：{}",e.getMessage());
            return "[HA]汇安支付生成签名异常";
        }
    }
}
