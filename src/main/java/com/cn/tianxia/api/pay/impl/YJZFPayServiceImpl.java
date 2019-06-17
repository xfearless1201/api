package com.cn.tianxia.api.pay.impl;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
 *
 * @ClassName YJZFPayServiceImpl
 * @Description 云捷支付
 * @author Hardy
 * @Date 2019年2月16日 上午11:53:58
 * @version 1.0.0
 */
public class YJZFPayServiceImpl  extends PayAbstractBaseService implements PayService {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(YJZFPayServiceImpl.class);

    private String merchId;//商户编号

    private String payUrl;//支付地址

    private String notifyUrl;//回调地址

    private String secret;//签名key

    private String queryOrderUrl;

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true

    public YJZFPayServiceImpl() {
    }

    public YJZFPayServiceImpl(Map<String,String> data) {
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

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        logger.info("[YJZF]云捷支付网银支付开始===============START============");
        try {
            //获取支付请求参数
            Map<String,String> data = sealRequest(payEntity, 1);
            //生成签名串
            String sign = generatorSign(data, 1);
            data.put("sign", sign);
            logger.info("[YJZF]云捷支付扫码支付请求参数报文:{}",JSONObject.fromObject(data).toString());
            String formStr = HttpUtils.get(data, payUrl);
            logger.info("[YJZF]云捷支付扫码支付生成form表单请求结果:{}",formStr);
            JSONObject jb = JSONObject.fromObject(formStr);
            if(jb.containsKey("status") && "1".equalsIgnoreCase(jb.getString("status"))){
//
                return PayResponse.wy_link(jb.getString("url"));
            }else {
                return PayResponse.error("支付下单失败"+ jb.getString("msg"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YJZF]云捷支付扫码支付异常:{}",e.getMessage());
            return PayResponse.error("[YJZF]云捷支付扫码支付异常");
        }
    }

    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[YJZF]云捷支付扫码支付开始===============START============");
        try {
            //获取支付请求参数
            Map<String,String> data = sealRequest(payEntity, 2);
            //生成签名串
            String sign = generatorSign(data, 1);
            data.put("sign", sign);
            logger.info("[YJZF]云捷支付扫码支付请求参数报文:{}",JSONObject.fromObject(data).toString());
            String formStr = HttpUtils.get(data, payUrl);
            logger.info("[YJZF]云捷支付扫码支付生成HTTP请求响应:{}",formStr);
            JSONObject jb = JSONObject.fromObject(formStr);
            if(jb.containsKey("status") && "1".equalsIgnoreCase(jb.getString("status"))){

                return PayResponse.sm_link(payEntity,jb.getString("url"),"下单成功");
            }else {
                return PayResponse.error("支付下单失败"+ jb.getString("msg"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YJZF]云捷支付扫码支付异常:{}",e.getMessage());
            return PayResponse.error("[YJZF]云捷支付扫码支付异常");
        }
    }

    @Override
    public String callback(Map<String, String> data) {
        logger.info("[YJZF]云捷支付回调验签开始===============START============");
        try {
            //获取签名串
            String sourceSign = data.remove("sign");
            logger.info("[YJZF]云捷支付回调验签获取原签名串:{}",sourceSign);
            String sign = generatorSign(data, 2);
            logger.info("[YJZF]云捷支付回调验签生成回调签名串:{}",sign);
            if(sign.equalsIgnoreCase(sourceSign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YJZF]云捷支付回调验签异常:{}",e.getMessage());
        }
        return "faild";
    }

    /**
     *
     * @Description 组装支付请求参数
     * @param entity
     * @param type 1 网银 2扫码
     * @return
     * @throws Exception
     */
    private Map<String,String> sealRequest(PayEntity entity,int type)throws Exception{
        logger.info("[YJZF]云捷支付组装支付请求参数开始============START==================");
        try {
            Map<String,String> data = new HashMap<>();
            String amount = new DecimalFormat("0.00").format(entity.getAmount());
            data.put("version","1.0");//版本号默认1.0
            data.put("customerid",merchId);//商户编号商户后台获取
            data.put("sdorderno",entity.getOrderNo());//商户订单号可用时间戳加随机数，不要超过18位
            data.put("total_fee",amount);//订单金额精确到小数点后两位，例如10.24
            if(type == 1){
                data.put("paytype","qbank");//支付编号详见支付表类目
                data.put("bankcode",entity.getPayCode());//银行编号详见银行编码，一般跳转收银台，
            }else{
                data.put("paytype",entity.getPayCode());//支付编号详见支付表类目
            }
            data.put("notifyurl", notifyUrl);//异步通知URL不能带有任何参数
            data.put("returnurl", "www.baidu.com");//同步跳转URL不能带有任何参数
//            data.put("remark","");//订单备注说明可以带上平台用户名或者充值 单号 可用于评定充值会员
            data.put("is_qrcode","3");//二维码/URL地址如果只想获取被扫二维码，请设置is_qrcode=1，已经废弃不适合非二维码支付或者跳转 is_qrcode=3 时获取支付地址返回json格式
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YJZF]云捷支付组装支付请求参数异常:{}",e.getMessage());
            throw new Exception("[YJZF]云捷支付组装支付请求参数异常");
        }
    }

    /**
     *
     * @Description 生成签名串
     * @param data
     * @param type
     * @return
     * @throws Exception
     */
    private String generatorSign(Map<String,String> data,int type) throws Exception{
        logger.info("[YJZF]云捷支付生成签名开始============START==================");
        try {
            StringBuffer sb = new StringBuffer();
            if(type == 1){
                //支付签名
                //签名规则
                //version={value}&customerid={value}&total_fee={value}&sdorderno={value}&
                //notifyurl={value}&returnurl={value}&{apikey}
                sb.append("version=").append(data.get("version")).append("&");
                sb.append("customerid=").append(data.get("customerid")).append("&");
                sb.append("total_fee=").append(data.get("total_fee")).append("&");
                sb.append("sdorderno=").append(data.get("sdorderno")).append("&");
                sb.append("notifyurl=").append(data.get("notifyurl")).append("&");
                sb.append("returnurl=").append(data.get("returnurl")).append("&");
            }else if( 2 == type){
                //回调签名
                //签名规则
                //customerid={value}&status={value}&sdpayno={value}&sdorderno={value}&
                //total_fee={value}&paytype={value}&{apikey}
                sb.append("customerid=").append(data.get("customerid")).append("&");
                sb.append("status=").append(data.get("status")).append("&");
                sb.append("sdpayno=").append(data.get("sdpayno")).append("&");
                sb.append("sdorderno=").append(data.get("sdorderno")).append("&");
                sb.append("total_fee=").append(data.get("total_fee")).append("&");
                sb.append("paytype=").append(data.get("paytype")).append("&");
            }else {
                //customerid={value}&sdorderno={value}&reqtime={value}&{apikey}
                sb.append("customerid=").append(data.get("customerid")).append("&");
                sb.append("sdorderno=").append(data.get("sdorderno")).append("&");
                sb.append("reqtime=").append(data.get("reqtime")).append("&");
            }
            String signStr = sb.append(secret).toString();
            logger.info("[YJZF]云捷支付生成待签名串:{}",signStr);
            //生成小写的32位密文
            String sign = MD5Utils.md5toUpCase_32Bit(signStr).toLowerCase();
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YJZF]云捷支付生成签名异常:{}",e.getMessage());
            throw new Exception("[YJZF]云捷支付生成签名异常");
        }
    }

    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        this.merchId = config.getString("merchId");
        this.notifyUrl = config.getString("notifyUrl");
        this.secret = config.getString("secret");
        this.queryOrderUrl = config.getString("queryOrderUrl");

        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);


        logger.info("[YJZF]云捷支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("YJZFNotify获取回调请求参数为空");
            return ret__failed;
        }

        String order_no = dataMap.get("sdorderno");// 平台订单号
        String trade_no = dataMap.get("sdpayno");// 平台订单号
        String trade_status = dataMap.get("status");//1:成功，其他失败
        String t_trade_status = "1";//“1” 为成功
        String amount= dataMap.get("total_fee");
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[YJZF]云捷支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }


        /**订单查询*/
        if(!queryOrder(order_no)) {
            logger.info("[YJZF]云捷支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }

        //写入数据库
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setRealAmount(Double.parseDouble(amount));//以“分”为单位
        processNotifyVO.setIp(ip);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setRet__success(ret__success);
        processNotifyVO.setInfoMap(JSONObject.fromObject(dataMap).toString());
        processNotifyVO.setT_trade_status(t_trade_status);
        processNotifyVO.setConfig(config);
        processNotifyVO.setPayment("YJZF");



        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[YJZF]云捷支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    private  Boolean queryOrder(String order_no){
        //订单查询
        try{
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("customerid", merchId);//
            queryMap.put("sdorderno", order_no);//
            queryMap.put("reqtime", time);//
            queryMap.put("sign", generatorSign(queryMap,3));//

            logger.info("[YJZF]云捷支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));

            String ponse = HttpUtils.toPostForm(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[YJZF]云捷支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            logger.info("[YJZF]云捷支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);

            if(jb.containsKey("status") && "1".equals(jb.getString("status"))){
                return true;
            }else {
                logger.info("[YJZF]云捷支付扫码支付回调查询订单{}订单信息：{}", order_no, jb.getString("msg"));
                return false;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[YJZF]云捷支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
            return false;
        }
    }
}
