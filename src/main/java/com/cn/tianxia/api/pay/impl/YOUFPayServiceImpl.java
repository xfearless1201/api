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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @ClassName YOUFPayServiceImpl
 * @Description 优付支付
 * @author vicky
 * @Date 2019年6月13日 下午19:20:58
 * @version 1.0.0
 */
public class YOUFPayServiceImpl extends PayAbstractBaseService implements PayService {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(YOUFPayServiceImpl.class);

    private String merchId;//商户编号

    private String payUrl;//支付地址

    private String notifyUrl;//回调地址

    private String secret;//签名key
    private String json;

    private String queryOrderUrl;

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true
    String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

    public YOUFPayServiceImpl() {
    }

    public YOUFPayServiceImpl(Map<String,String> data) {
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
            if(data.containsKey("json")){
                this.json = data.get("json");
            }
            if(data.containsKey("queryOrderUrl")){
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
        }
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        logger.info("[YOUF]优付支付网银支付开始===============START============");
        try {
            //获取支付请求参数
            Map<String,String> data = sealRequest(payEntity, 1);
            //生成签名串
            String sign = generatorSign(data, 1);
            data.put("sign", sign);
            logger.info("[YOUF]优付支付扫码支付请求参数报文:{}",JSONObject.fromObject(data).toString());

            if("1" .equalsIgnoreCase(json)){
                String postStr = HttpUtils.toPostForm(data, payUrl);
                logger.info("[YOUF]优付支付扫码支付生成form表单请求结果:{}",postStr);
                JSONObject jb = JSONObject.fromObject(postStr);
                if(jb.containsKey("status") && "1".equalsIgnoreCase(jb.getString("status"))){

                    return PayResponse.wy_link(jb.getString("next_url"));
                }else {

                    return PayResponse.error("支付下单失败"+ jb.getString("msg"));
                }
            }else {
                String formStr = HttpUtils.generatorForm(data, payUrl);
                return PayResponse.wy_write(formStr);
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YOUF]优付支付扫码支付异常:{}",e.getMessage());
            return PayResponse.error("[YOUF]优付支付扫码支付异常");
        }
    }

    @Override
    public JSONObject smPay(PayEntity payEntity) {
        logger.info("[YOUF]优付支付扫码支付开始===============START============");
        try {
            //获取支付请求参数
            Map<String,String> data = sealRequest(payEntity, 2);
            //生成签名串
            String sign = generatorSign(data, 1);
            data.put("sign", sign);
            logger.info("[YOUF]优付支付扫码支付请求参数报文:{}",JSONObject.fromObject(data).toString());
            if("1" .equalsIgnoreCase(json)){
                String postStr = HttpUtils.toPostForm(data, payUrl);
                logger.info("[YOUF]优付支付扫码支付生成form表单请求结果:{}",postStr);
                JSONObject jb = JSONObject.fromObject(postStr);
                if(jb.containsKey("status") && "1".equalsIgnoreCase(jb.getString("status"))){

                    return PayResponse.sm_link(payEntity,jb.getString("next_url"),"下单成功");
                }else {

                    return PayResponse.error("支付下单失败"+ jb.getString("msg"));
                }
            }else {
                String formStr = HttpUtils.generatorForm(data, payUrl);
                return PayResponse.sm_form(payEntity,formStr,"下单成功");
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YOUF]优付支付扫码支付异常:{}",e.getMessage());
            return PayResponse.error("[YOUF]优付支付扫码支付异常");
        }
    }

    @Override
    public String callback(Map<String, String> data) {
        logger.info("[YOUF]优付支付回调验签开始===============START============");
        try {
            //获取签名串
            String sourceSign = data.remove("sign");
            logger.info("[YOUF]优付支付回调验签获取原签名串:{}",sourceSign);
            String sign = generatorSign(data, 2);
            logger.info("[YOUF]优付支付回调验签生成回调签名串:{}",sign);
            if(sign.equalsIgnoreCase(sourceSign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YOUF]优付支付回调验签异常:{}",e.getMessage());
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
        logger.info("[YOUF]优付支付组装支付请求参数开始============START==================");
        try {
            Map<String,String> data = new HashMap<>();
            String amount = new DecimalFormat("0.00").format(entity.getAmount());

            data.put("version","1.0");//版本号是是varchar(5)1.0默认1.0
            data.put("customerid", merchId);//商户ID是是int(8)10000商户后台获取
            data.put("sdorderno",entity.getOrderNo());//商户订单号是是varchar(20)AA100000数字或字母，不允许中文、不允许重复
            data.put("total_fee", amount);//订单金额是是decimal(10,2)100.01精确到小数点后两位，例如10.24
            data.put("notifyurl",notifyUrl);//异步通知地址是是varchar(50)http://www.xxx.com/notify/用于向该地址提交订单支付状态
            data.put("returnurl","www.baidu.com");//同步跳转地址是是varchar(50)http://www.xxx.com/returl/用户支付状态改变后跳转该地址，并给予参数做相应响应
            if(type == 1){
                data.put("paytype","bank");//支付编号详见支付表类目
                data.put("bankcode",entity.getPayCode());//银行编号详见银行编码，一般跳转收银台，
            }else{
                data.put("paytype",entity.getPayCode());//支付编号详见支付表类目
            }
            data.put("json",json);//获取json格式的支付信息
            data.put("sign",generatorSign(data, 1));//
            data.put("remark","TOP-UP");//商户备注否否varchar(50)备注信息异步通知含有该参数，作用自行考虑
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YOUF]优付支付组装支付请求参数异常:{}",e.getMessage());
            throw new Exception("[YOUF]优付支付组装支付请求参数异常");
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
        logger.info("[YOUF]优付支付生成签名开始============START==================");
        try {
            StringBuffer sb = new StringBuffer();
            if(type == 1){
                //支付签名
                //签名规则 version={value}&customerid={value}&total_fee={value}&sdorderno={value}&notifyurl={value}&returnurl={value}&{apikey}
                sb.append("version=").append(data.get("version")).append("&");
                sb.append("customerid=").append(data.get("customerid")).append("&");
                sb.append("total_fee=").append(data.get("total_fee")).append("&");
                sb.append("sdorderno=").append(data.get("sdorderno")).append("&");
                sb.append("notifyurl=").append(data.get("notifyurl")).append("&");
                sb.append("returnurl=").append(data.get("returnurl")).append("&");
            }else if( 2 == type){
                //回调签名
                //签名规则
                // customerid={value}&status={value}&sdpayno={value}&sdorderno={value}&total_fee={value}&paytype={value}&{apikey}
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
            logger.info("[YOUF]优付支付生成待签名串:{}",signStr);
            //生成小写的32位密文
            String sign = MD5Utils.md5toUpCase_32Bit(signStr).toLowerCase();
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[YOUF]优付支付生成签名异常:{}",e.getMessage());
            throw new Exception("[YOUF]优付支付生成签名异常");
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


        logger.info("[YOUF]优付支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("YOUFNotify获取回调请求参数为空");
            return ret__failed;
        }

        String order_no = dataMap.get("sdorderno");// 我司订单号
        String trade_no = dataMap.get("sdpayno");// 平台订单号
        String trade_status = dataMap.get("status");//1:成功，其他失败
        String t_trade_status = "1";//“1” 为成功
        String amount= dataMap.get("total_fee");
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[YOUF]优付支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }


        /**订单查询*/
        if(!queryOrder(order_no)) {
            logger.info("[YOUF]优付支付扫码支付回调查询订单{}失败", order_no);
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
        processNotifyVO.setPayment("YOUF");



        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[YOUF]优付支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    private  Boolean queryOrder(String order_no){
        //订单查询
        try{

            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("customerid", merchId);//
            queryMap.put("sdorderno", order_no);//
            queryMap.put("reqtime", time);//
            queryMap.put("sign", generatorSign(queryMap,3));//

            logger.info("[YOUF]优付支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));

            String ponse = HttpUtils.toPostForm(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[YOUF]优付支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            logger.info("[YOUF]优付支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);
            //0|1 0 错误、1 支付成功
            if(jb.containsKey("status") && "1".equals(jb.getString("status"))){
                return true;
            }else {
                logger.info("[YOUF]优付支付扫码支付回调查询订单{}订单信息：{}", order_no, jb.getString("msg"));
                return false;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[YOUF]优付支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
            return false;
        }
    }
}
