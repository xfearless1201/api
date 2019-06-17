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
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vicky
 * @version 1.0.0
 * @ClassName HENGPayServiceImpl
 * @Description
 * @Date 2019/5/29 09 10
 **/
public class HENGPayServiceImpl extends PayAbstractBaseService implements PayService {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(HENGPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String kjpayUrl;//快捷支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;
    private String version;

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true


    public HENGPayServiceImpl() {
    }

    public HENGPayServiceImpl(Map<String, String> data) {
        if(MapUtils.isNotEmpty(data)){
            if(data.containsKey("merchId")){
                this.merchId = data.get("merchId");
            }if(data.containsKey("payUrl")){
                this.payUrl = data.get("payUrl");
            }
            if(data.containsKey("kjpayUrl")){
                this.kjpayUrl = data.get("kjpayUrl");
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
            if(data.containsKey("version")){
                this.version = data.get("version");
            }
        }
    }
    /**
     * 回调
     */
    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        this.merchId = config.getString("merchId");
        this.notifyUrl = config.getString("notifyUrl");
        this.secret = config.getString("secret");
        this.queryOrderUrl = config.getString("queryOrderUrl");

        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);
        logger.info("[HENG]恒通支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("HENGNotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("systemorderid");//第三方订单号，流水号
        String order_no = dataMap.get("orderid");//支付订单号
        String amount = dataMap.get("payamount");//终端支付用户实际支付金额，依据此金额上分
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[HENG]恒通支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        //0：未支付 1：支付成功 2：失败
        String trade_status = dataMap.get("result");  //第三方支付状态，1 支付成功
        String t_trade_status = "1";//第三方成功状态

        //订单查询
        try{
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("orderid", order_no);
            queryMap.put("customer", merchId);
            queryMap.put("sign", generatorSign(queryMap, 3));

            logger.info("[HENG]恒通支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));
            String ponse = HttpUtils.get(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[HENG]恒通支付回调查询订单发起HTTP请求无响应");
                return ret__failed;
            }
            logger.info("[HENG]恒通支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);
            //0:待支付，1支付中，2: 已支付，3:已支付、通知失败,4：支付失败',
            if(jb.containsKey("code") && "0".equals(jb.getString("code"))){
                JSONObject json = jb.getJSONObject("obj");

                //-1 订单 0：未支付 1：支付成功 2：失败
                if(!"1".equals(json.getString("result"))){
                    logger.info("[HENG]恒通支付扫码支付回调查询订单,请求状态为:{}", json.getString("result"));
                    return ret__failed;
                }
            }else {
                logger.info("[HENG]恒通支付扫码支付回调查询订单,请求状态为:{}", jb.getString("msg"));
                return ret__failed;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[HENG]恒通支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
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
        processNotifyVO.setPayment("HENG");

        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[HENG]恒通支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }
    /**
     * 网银支付
     */
    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        return null;
    }

    /**
     * 扫码支付
     */
    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[HENG]恒通支付扫码支付开始===================START=======================");
        try {
            Map<String,String> map = null;
            String responseData = null;

            if("kj".equalsIgnoreCase(payEntity.getPayCode())){
                //快捷支付
                map = sealRequest(payEntity);//再
                map.remove("banktype");
                responseData = HttpUtils.generatorForm(map, kjpayUrl);

                logger.info("[HENG]恒通支付扫码支付请求网关：{}", kjpayUrl);
            }else {
                map = sealRequest(payEntity);

                responseData = HttpUtils.generatorForm(map, payUrl);

                logger.info("[HENG]恒通支付扫码支付请求网关：{}", payUrl);
            }
            logger.info("[HENG]恒通支付扫码支付请求参数：{}", JSONObject.fromObject(map));

            return PayResponse.sm_form(payEntity,responseData,"下单成功");

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[HENG]恒通支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[HENG]恒通支付扫码支付异常");
        }
    }

    /**
     * 异步回调接口
     */
    @Override
    public String callback(Map<String, String> data) {
        logger.info("[HENG]恒通支付回调验签开始===================START==============");
        try {
            //获取验签原串
            String sourceSign = data.get("sign");
            //生成待签名串
            String sign = generatorSign(data, 2);
            logger.info("[HENG]恒通支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            //验签
            if(sourceSign.equalsIgnoreCase(sign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[HENG]恒通支付回调验签异常:"+e.getMessage());
            return ret__failed;
        }
        return ret__failed;
    }


    /**
     *
     * @Description 封装支付请求参数
     * @param entity
     * @param
     * @return
     * @throws Exception
     */
    private Map<String,String> sealRequest(PayEntity entity) throws Exception{
        logger.info("[BL]宝来支付组装支付请求参数开始===================START==================");
        try {

            String amount = new DecimalFormat("0.00").format(entity.getAmount());
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            //创建存储支付请求参数对象
            Map<String,String> dataMap = new HashMap<>();
            dataMap.put("customer", merchId);//商户ID
            dataMap.put("banktype", entity.getPayCode());//支付类型
            dataMap.put("amount", amount);//金额
            dataMap.put("orderid", entity.getOrderNo());//商户订单号，不大于30位
            dataMap.put("asynbackurl", notifyUrl);//异步通知地址
            dataMap.put("request_time", time);//请求时间
            dataMap.put("synbackurl", entity.getRefererUrl());//同步通知地址
            dataMap.put("attach", "TOP-UP");//备注消息
            dataMap.put("sign", generatorSign(dataMap, 1));//MD5签名

            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[HENG]恒通支付组装支付请求参数异常:"+e.getMessage());
            throw new Exception("组装支付请求参数异常!");
        }
    }


    /**
     *
     * @Description 生成签名串
     * @param data
     * @return
     * @throws Exception
     */
    private String generatorSign(Map<String,String> data, int type) throws Exception{
        logger.info("[HENG]恒通支付生成签名串开始================START=================");
        try {
            StringBuffer sb = new StringBuffer();
            if(1 == type){
                //发起支付
                if("kj".equalsIgnoreCase(data.get("banktype"))){
                    //快捷支付
                    //customer={0}&amount={1}&orderid={2}&asynbackurl={3}&request_time={4}&key={5}
                    sb.append("customer=").append(data.get("customer")).append("&");
                    sb.append("amount=").append(data.get("amount")).append("&");
                    sb.append("orderid=").append(data.get("orderid")).append("&");
                    sb.append("asynbackurl=").append(data.get("asynbackurl")).append("&");
                    sb.append("request_time=").append(data.get("request_time")).append("&");
                }else {
                    //customer={0}&banktype={1}&amount={2}&orderid={3}&asynbackurl={4}&request_time={5}&key={6}
                    sb.append("customer=").append(data.get("customer")).append("&");
                    sb.append("banktype=").append(data.get("banktype")).append("&");
                    sb.append("amount=").append(data.get("amount")).append("&");
                    sb.append("orderid=").append(data.get("orderid")).append("&");
                    sb.append("asynbackurl=").append(data.get("asynbackurl")).append("&");
                    sb.append("request_time=").append(data.get("request_time")).append("&");
                }

            }else if(2 == type){
                //orderid={0}&result={1}&amount={2}&systemorderid={3}&completetime={4}&key={5}
                sb.append("orderid=").append(data.get("orderid")).append("&");
                sb.append("result=").append(data.get("result")).append("&");
                sb.append("amount=").append(data.get("amount")).append("&");
                sb.append("systemorderid=").append(data.get("systemorderid")).append("&");
                sb.append("completetime=").append(data.get("completetime")).append("&");

            }else {//订单查询
                //orderid={0}&customer={1}&key={2}
                sb.append("orderid=").append(data.get("orderid")).append("&");
                sb.append("customer=").append(data.get("customer")).append("&");
            }

            sb.append("key=").append(secret);

            String strString = sb.toString();
            logger.info("[HENG]恒通支付扫码支付生成待签名串：{}",strString);
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString()).toLowerCase();

            logger.info("[HENG]恒通支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[HENG]恒通支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[HENG]恒通支付生成签名串异常!");
        }
    }
}
