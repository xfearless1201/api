package com.cn.tianxia.api.pay.impl;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @ClassName BLPayServiceImpl
 * @Description BL宝来支付对接
 * @Author kay
 * @Date 2018年10月11日 20:51
 * @Version 1.0.0
 **/
public class BLPayServiceImpl extends PayAbstractBaseService implements PayService {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(BLPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;
    private String version;

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true


    public BLPayServiceImpl() {
    }

    public BLPayServiceImpl(Map<String, String> data) {
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
        logger.info("[BL]宝来支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("BLNotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("plat_trade_no");//第三方订单号，流水号
        String order_no = dataMap.get("out_trade_no");//支付订单号
        String amount = dataMap.get("real_fee");//实际支付金额,以分为单位
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[BL]宝来支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        String trade_status = "0";  //第三方支付状态，1 支付成功
        String t_trade_status = "0";//第三方成功状态

        //订单查询
        try{
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("ordernum", order_no);

            logger.info("[BL]宝来支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));
            String ponse = HttpUtils.toPostForm(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[BL]宝来支付回调查询订单发起HTTP请求无响应");
                return ret__failed;
            }
            logger.info("[BL]宝来支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);
            //0:待支付，1支付中，2: 已支付，3:已支付、通知失败,4：支付失败',
            if( "0".equals(jb.getString("state")) || "4".equals(jb.getString("state"))){
                logger.info("[BL]宝来支付扫码支付回调查询订单,请求状态为:{}", jb.getString("state"));
                return ret__failed;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[BL]宝来支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
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
        processNotifyVO.setPayment("BL");

        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[BL]宝来支付回调验签失败");
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
        logger.info("[BL]宝来支付扫码支付开始===================START=======================");
        try {
            Map<String,String> map = sealRequest(payEntity);

            String responseData = HttpUtils.generatorForm(map, payUrl);

            if(StringUtils.isBlank(responseData)){
                logger.info("[BL]宝来支付发起HTTP请求无响应");
                return PayResponse.error("[BL]宝来支付扫码支付发起HTTP请求无响应");
            }
            return PayResponse.sm_form(payEntity,responseData,"下单成功");

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[BL]宝来支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[BL]宝来支付扫码支付异常");
        }
    }

    /**
     * 异步回调接口
     */
    @Override
    public String callback(Map<String, String> data) {
        logger.info("[BL]宝来支付回调验签开始===================START==============");
        try {
            //获取验签原串
            String sourceSign = data.get("sign");
            //生成待签名串
            String sign = generatorSign(data);
            logger.info("[BL]宝来支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            //验签
            if(sourceSign.equalsIgnoreCase(sign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[BL]宝来支付回调验签异常:"+e.getMessage());
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
            //创建存储支付请求参数对象
            Map<String,String> map = new HashMap<>();

            map.put("mer_id", merchId);//商户号
            map.put("out_trade_no", entity.getOrderNo());//商户订单号
            map.put("pay_type", entity.getPayCode());//支付类型
            map.put("amount", amount);//交易金额
            //txk 平台要求在支付成功后页面转跳到会员中心
            if("http://www.99hh.org/".equalsIgnoreCase(entity.getRefererUrl()) ){
                map.put("callback_url", entity.getRefererUrl()+"UserCenter/member_index.html");//前台回调URL
            } else if("http://m.99hh.org/".equalsIgnoreCase(entity.getRefererUrl())){
                map.put("callback_url", entity.getRefererUrl()+"MemberCentre");//前台回调URL
            }else {
                map.put("callback_url", entity.getRefererUrl());
            }
            map.put("notify_url", notifyUrl);//后台同步URL
            map.put("version", version);//版本号
            map.put("sign", generatorSign(map));//签名

            logger.info("[BL]宝来支付扫码支付请求参数：{}", JSONObject.fromObject(map));
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[BL]宝来支付组装支付请求参数异常:"+e.getMessage());
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
    private String generatorSign(Map<String,String> data) throws Exception{
        logger.info("[BL]宝来支付生成签名串开始================START=================");
        try {

            TreeMap<String, String> treeMap = new TreeMap<>(data);
            StringBuffer sb = new StringBuffer();
            Iterator<String> iterator = treeMap.keySet().iterator();
            while (iterator.hasNext()){
                String key = iterator.next();
                String val = treeMap.get(key);
                if(StringUtils.isBlank(val) || "sign".equals(key)){
                    continue;
                }
                sb.append(key).append("=").append(val).append("&");
            }
            sb.append("key=").append(secret);

            String strString = sb.toString();
            logger.info("[BL]宝来支付扫码支付生成待签名串：{}",strString);
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString()).toLowerCase();

            logger.info("[BL]宝来支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[BL]宝来支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[BL]宝来支付生成签名串异常!");
        }
    }
}
