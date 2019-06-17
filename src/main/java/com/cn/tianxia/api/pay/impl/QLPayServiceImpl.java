/****************************************************************** 
 *
 *    Powered By tianxia-online. 
 *
 *    Copyright (c) 2018-2020 Digital Telemedia 天下科技 
 *    http://www.d-telemedia.com/ 
 *
 *    Package:     com.cn.tianxia.pay.impl 
 *
 *    Filename:    QLPayServiceImpl.java 
 *
 *    Description: TODO(用一句话描述该文件做什么) 
 *
 *    Copyright:   Copyright (c) 2018-2020 
 *
 *    Company:     天下科技 
 *
 *    @author:    Roman 
 *
 *    @version:    1.0.0 
 *
 *    Create at:   2019年03月12日 20:16 
 *
 *    Revision: 
 *
 *    2019/3/12 20:16 
 *        - first revision 
 *
 *****************************************************************/

package com.cn.tianxia.api.pay.impl;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 *  * @ClassName QLPayServiceImpl
 *  * @Description TODO(麒麟支付)
 *  * @Author Roman
 *  * @Date 2019年03月12日 20:16
 *  * @Version 1.0.0
 *  2019年05月15日 14:40  重新对接(支付商接口有变)
 **/

public class QLPayServiceImpl extends PayAbstractBaseService implements PayService {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(QLPayServiceImpl.class);

    private static final String RET_FAILED = "fail";

    private static final String RET_SUCCESS = "SUCCESS";

    /**
     * 商户号
     */
    private String merchId;
    /**
     * 服务号
     */
    private String spId;

    /**
     * 支付请求地址
     */

    private String payUrl;

    /**
     * 回调地址
     */
    private String notifyUrl;

    /**
     * 密钥
     */
    private String secret;

    /**
     * 订单查询地址
     */
    private String queryOrderUrl;


    /**
     * 构造器，初始化参数
     */
    public QLPayServiceImpl() {
    }

    public QLPayServiceImpl(Map<String, String> data) {
        if (MapUtils.isNotEmpty(data)) {
            if (data.containsKey("merchId")) {
                this.merchId = data.get("merchId");
            }
            if (data.containsKey("payUrl")) {
                this.payUrl = data.get("payUrl");
            }
            if (data.containsKey("notifyUrl")) {
                this.notifyUrl = data.get("notifyUrl");
            }
            if (data.containsKey("secret")) {
                this.secret = data.get("secret");
            }
            if (data.containsKey("queryOrderUrl")) {
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
            if (data.containsKey("spId")) {
                this.spId = data.get("spId");
            }
        }
    }


    @Override
    public JSONObject wyPay(PayEntity entity) {
        return null;
    }

    @Override
    public String callback(Map<String, String> data) {
        return null;
    }

    @Override
    public JSONObject smPay(PayEntity entity) {
        logger.info("[QL]麒麟支付扫码支付开始============START======================");
        try {
            //获取支付请求参数
            Map<String, String> data = sealRequest(entity);

            String url = payUrl+entity.getPayCode();
            System.out.println("支付请求地址：" + url);
            //发送请求
            String responseData = HttpUtils.toPostJsonStr(JSONObject.fromObject(data), url);
            logger.info("[QL]麒麟支付扫码支付响应:{}", JSONObject.fromObject(responseData));
            if (StringUtils.isBlank(responseData)) {
                logger.info("[QL]麒麟支付扫码支付发起HTTP请求无响应结果");
                return PayResponse.error("[QL]麒麟支付扫码支付发起HTTP请求无响应结果");
            }
            //解析支付商返回信息
            JSONObject json = JSONObject.fromObject(responseData);
            if(json.containsKey("status") && "0".equals(json.getString("status"))){
                if(StringUtils.isBlank(entity.getMobile())){
                    //PC
                    return PayResponse.sm_qrcode(entity, json.getString("qrCode"), "下单成功");
                }else {
                    return PayResponse.sm_link(entity, json.getString("qrCode"), "下单成功");
                }
            }
            return PayResponse.error("下单失败");

        } catch (Exception e) {
            e.printStackTrace();
            return PayResponse.error("[QL]麒麟支付扫码支付下单失败" + e.getMessage());
        }
    }

    /**
     * 功能描述:回调验签
     *
     * @param data 回调请求参数
     * @Date: 2019年03月12日 20:59:51
     * @return: boolean
     **/
    private boolean verifyCallback(Map<String, String> data) {
        logger.info("[QL]麒麟支付回调验签开始==============START===========");

        //获取回调通知原签名串
        String sourceSign = data.remove("sign");

        //生成验签签名串
        String sign = null;
        try {
            sign = generatorSign(data);

            logger.info("[WOTC]WOTC支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[QL]麒麟支付生成加密串异常:{}", e.getMessage());
        }
        return sourceSign.equalsIgnoreCase(sign);
    }

    /**
     * @param
     * @return
     * @throws Exception
     * @Description 封装支付请求参数
     */
    private Map<String, String> sealRequest(PayEntity entity) throws Exception {
        logger.info("[QL]麒麟支付封装请求参数开始=====================START==================");
        try {
            //创建支付请求参数存储对象
            Map<String, String> dataMap = new HashMap<>();
            //订单金额
            String amount = new DecimalFormat("##").format(entity.getAmount()*100);

            dataMap.put("mcht_no", merchId);//商户编码
            dataMap.put("trade_no", entity.getOrderNo());//交易流水号
            dataMap.put("notify_url", notifyUrl);//交易通知地址
            dataMap.put("totalAmount", amount);//订单金额，单位：分
            dataMap.put("subject",  "TOP-UP");//订单标题
            dataMap.put("sign", generatorSign(dataMap));//签名

            logger.info("[QL]麒麟支付扫码支付请求参数：{}", JSONObject.fromObject(dataMap));
            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[QL]麒麟支付封装请求参数异常:" + e.getMessage());
            throw new Exception("封装支付请求参数异常!");
        }
    }

    /**
     * @param data
     * @return
     * @throws Exception
     * @Description 生成支付签名串
     */
    public String generatorSign(Map<String, String> data) throws Exception {
        logger.info("[QL]麒麟支付生成支付签名串开始==================START========================");
        try {
            //除 sign 字段外，所有参数按照字段名的 ascii  码从小到大排序后使用 QueryString 的格式（即
            //key1=value1&key2=value2…）拼接而成，空值不传递，不参与签名组串。

            Map<String, String> sortMap = MapUtils.sortByKeys(data);
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
            String strString = sb.toString();
            logger.info("[QL]麒麟支付扫码支付生成待签名串：{}",strString);
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString());

            logger.info("[QL]麒麟支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[QL]麒麟支付生成支付签名串异常:" + e.getMessage());
            throw new Exception("生成支付签名串异常!");
        }
    }

    /**
     * 回调方法
     *
     * @param request  第三方请求request
     * @param response response
     * @param config   平台对应支付商配置信息
     * @return
     */
    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        //参数验签，从配置中获取
        this.secret = config.getString("secret");
        this.merchId = config.getString("merchId");
        this.queryOrderUrl = config.getString("queryOrderUrl");
        this.notifyUrl = config.getString("notifyUrl");
        this.spId = config.getString("spId");

        //获取回调请求参数
        Map<String, String> infoMap = ParamsUtils.getNotifyParams(request);
        //支付商 返回的信息在KEY里面，不在value里
        JSONObject jsonObject = JSONObject.fromObject(infoMap);
        Iterator<String> keys = jsonObject.keys();
        if(keys.hasNext()) {
            infoMap = JSONObject.fromObject(keys.next());
        }

        logger.info("[QL]麒麟支付扫码支付回调请求参数：{}", JSONObject.fromObject(infoMap));
        if (!MapUtils.isNotEmpty(infoMap)) {
            logger.error("QLNotify获取回调请求参数为空");
            return RET_FAILED;
        }

        // 平台订单号
        String orderNo = infoMap.get("transactionId");
        // 第三方订单号
        String tradeNo = infoMap.get("sysNo");
        //订单状态
        String tradeStatus = infoMap.get("resultCode");
        // 表示成功状态
        String tTradeStatus = "0";
        //实际支付金额
        String orderAmount = infoMap.get("totalAmount");

        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        //订单查询
        try{
            Map<String,String> queryMap = new HashMap<>();
            queryMap.put("sp_id", spId);//服务商号
            queryMap.put("mch_id",merchId);//商户号
            queryMap.put("out_trade_no", orderNo);//商户订单号
            queryMap.put("nonce_str", String.valueOf(System.currentTimeMillis()));//随机字符串
            queryMap.put("sign", generatorSign(queryMap));//签名
            logger.info("[QL]麒麟支付扫码支付回调查询订单{}请求参数：{}", orderNo, JSONObject.fromObject(queryMap));

            String quryData = HttpUtils.toPostJsonStr(JSONObject.fromObject(queryMap), queryOrderUrl);
            if(StringUtils.isBlank(quryData)){
                logger.info("[QL]麒麟支付回调查询订单发起HTTP请求无响应");
                return ret__failed;
            }

            logger.info("[QL]麒麟支付扫码支付回调查询订单{}响应信息：{}", orderNo, JSONObject.fromObject(quryData));
            JSONObject jb = JSONObject.fromObject(quryData);

            if(jb.containsKey("status") && "SUCCESS".equals(jb.getString("status"))){

                //1、订单生成 2、订单支付中 3、订单支付成功 4、订单支付失败
                if(!"3".equals(jb.getString("trade_state"))){

                    logger.info("[QL]麒麟支付扫码支付回调查询订单,订单支付状态为:{}",jb.getString("trade_state"));
                    return ret__failed;
                }
            }else {
                logger.info("[QL]麒麟支付扫码支付回调查询订单,请求状态为:{}",jb.getString("status"));
                return ret__failed;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[QL]麒麟支付扫码支付回调查询订单{}异常{}：",orderNo,e.getMessage());
            return ret__failed;
        }

        //回调验签
        boolean verifyRequest = verifyCallback(infoMap);

        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        //成功返回
        processNotifyVO.setRet__success(RET_SUCCESS);
        //失败返回
        processNotifyVO.setRet__failed(RET_FAILED);
        processNotifyVO.setIp(ip);
        processNotifyVO.setOrder_no(orderNo);
        processNotifyVO.setTrade_no(tradeNo);
        processNotifyVO.setTrade_status(tradeStatus);
        processNotifyVO.setT_trade_status(tTradeStatus);
        processNotifyVO.setRealAmount(Double.parseDouble(orderAmount)/100);
        //回调参数
        processNotifyVO.setInfoMap(JSONObject.fromObject(infoMap).toString());
        processNotifyVO.setPayment("QL");
        processNotifyVO.setConfig(config);

        return super.processSuccessNotify(processNotifyVO, verifyRequest);
    }
}

