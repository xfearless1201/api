/****************************************************************** 
 *
 *    Powered By tianxia-online. 
 *
 *    Copyright (c) 2018-2020 Digital Telemedia 天下科技 
 *    http://www.d-telemedia.com/ 
 *
 *    Package:     com.cn.tianxia.pay.impl 
 *
 *    Filename:    XYXPayServiceImpl.java 
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
 *    Create at:   2019年05月03日 18:39 
 *
 *    Revision: 
 *
 *    2019/5/3 18:39 
 *        - first revision 
 *
 *****************************************************************/

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
 *  * @ClassName XYXPayServiceImpl
 *  * @Description TODO(逍遥仙支付)
 *  * @Author Roman
 *  * @Date 2019年05月03日 18:39
 *  * @Version 1.0.0
 *  
 **/

public class XYXPayServiceImpl extends PayAbstractBaseService implements PayService {
    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(XYXPayServiceImpl.class);

    private static final String ret__failed = "Notify Is Failed";

    private static final String ret__success = "OK";

    /**
     * 商户号
     */
    private String merchId;

    /**
     * 支付地址
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
    public XYXPayServiceImpl() {
    }

    public XYXPayServiceImpl(Map<String, String> data) {
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
        }
    }

    @Override
    public JSONObject wyPay(PayEntity payEntity) {
        return null;
    }

    @Override
    public String callback(Map<String, String> data) {
        return null;
    }

    @Override
    public JSONObject smPay(PayEntity entity) {
        try {
            //获取支付请求参数
            Map<String, String> data = sealRequest(entity);
            logger.info("[XYX]逍遥仙支付扫码支付请求参数报文:{}", JSONObject.fromObject(data));

            String response = HttpUtils.generatorForm(data, payUrl);
            logger.info("[XYX]逍遥仙支付扫码支付响应参数：{}", response);
            if (StringUtils.isBlank(response)) {
                logger.info("[XYX]逍遥仙支付扫码支付发起HTTP请求无响应");
                return PayResponse.error("[XYX]逍遥仙支付扫码支付发起HTTP请求无响应");
            }
            return PayResponse.sm_form(entity,response,"下单成功");
        } catch (Exception e) {
            e.printStackTrace();
            return PayResponse.error("[XYX]逍遥仙支付扫码支付异常" + e.getMessage());
        }
    }

    /**
     * @param entity
     * @return
     * @throws Exception
     * @Description 封装支付请求参数
     */
    private Map<String, String> sealRequest(PayEntity entity) throws Exception {
        logger.info("[XYX]逍遥仙支付封装请求参数开始=====================START==================");
        try {
            //创建支付请求参数存储对象
            Map<String, String> dataMap = new HashMap<>();
            //订单金额
            String amount = new DecimalFormat("0.00").format(entity.getAmount());
            //订单号
            String orderNo = entity.getOrderNo();
            //下单时间
            String orderTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());


//            pay_memberid	商户号	是	是	平台分配商户号
            dataMap.put("pay_memberid", merchId);

//            pay_orderid	订号	是	是	上送订单号唯一, 字符长度20
            dataMap.put("pay_orderid", orderNo);

//            pay_applydate	提交时间	是	是	时间格式：2016-12-26 18:18:18
            dataMap.put("pay_applydate", orderTime);

//            pay_bankcode	银行编码	是	是	参考后续说明
            dataMap.put("pay_bankcode", entity.getPayCode());

//            pay_notifyurl	服务端通知	是	是	服务端返回地址.（POST返回数据）
            dataMap.put("pay_notifyurl", notifyUrl);

//            pay_callbackurl	页面跳转通知	是	是	页面跳转返回地址（POST返回数据）
            dataMap.put("pay_callbackurl", entity.getRefererUrl());

//            pay_amount	订单金额	是	是	商品金额
            dataMap.put("pay_amount", amount);

            //生成待签名串
            String sign = generatorSign(dataMap);
//            pay_md5sign	MD5签名	是	否	请看MD5签名字段格式
            dataMap.put("pay_md5sign", sign);

//            pay_productname	商品名称	是	否
            dataMap.put("pay_productname", "top_Up");

            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[XYX]逍遥仙支付封装请求参数异常:" + e.getMessage());
            throw new Exception("封装支付请求参数异常!");
        }
    }

    /**
     * @param
     * @return
     * @throws Exception
     * @Description 生成支付签名串
     */
    public String generatorSign(Map<String, String> data) throws Exception {
        try {
            Map<String, String> sortMap = MapUtils.sortByKeys(data);
            StringBuffer sb = new StringBuffer();
            for (String key : sortMap.keySet()) {
                String val = sortMap.get(key);
                if (StringUtils.isBlank(val) || "sign".equalsIgnoreCase(key)
                        || "attach".equalsIgnoreCase(key)) {
                    continue;
                }
                sb.append(key).append("=").append(val).append("&");
            }
            sb.append("key=").append(secret);
            //生成待签名串
            String signStr = sb.toString();
            logger.info("[XYX]逍遥仙支付生成待签名串:" + signStr);
            //生成加密串
            String sign = MD5Utils.md5toUpCase_32Bit(signStr);
            logger.info("[XYX]逍遥仙支付生成签名串:" + sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[XYX]逍遥仙支付生成支付签名串异常:" + e.getMessage());
            throw new Exception("生成支付签名串异常!");
        }
    }

    /**
     * 功能描述:回调验签
     *
     * @param data
     * @return: boolean
     **/
    private boolean verifyCallback(Map<String, String> data) {
        logger.info("[XYX]逍遥仙支付回调验签开始==============START===========");

        //获取回调通知原签名串
        String sourceSign = data.remove("sign");
        logger.info("[XYX]逍遥仙支付回调验签获取原签名串:{}", sourceSign);
        //生成验签签名串
        String sign = null;
        try {
            sign = generatorSign(data);
            logger.info("[XYX]逍遥仙支付回调验签生成加密串:{}", sign);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XYX]逍遥仙支付生成加密串异常:{}", e.getMessage());
        }
        return sourceSign.equalsIgnoreCase(sign);
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
        //获取回调请求参数
        Map<String, String> infoMap = ParamsUtils.getNotifyParams(request);

        logger.info("[XYX]逍遥仙支付回调请求参数:{}", JSONObject.fromObject(infoMap));
        if (!MapUtils.isNotEmpty(infoMap)) {
            logger.error("获取回调请求参数为空");
            return ret__failed;
        }

        boolean verifyRequest = verifyCallback(infoMap);

        // 平台订单号
        String orderNo = infoMap.get("orderid");
        // 第三方订单号
        String tradeNo = infoMap.get("transaction_id");
        //订单状态
        String tradeStatus = infoMap.get("returncode");
        // 表示成功状态
        String tTradeStatus = "00";
        //实际支付金额
        String orderAmount = infoMap.get("amount");
        if (StringUtils.isBlank(orderAmount)) {
            logger.info("获取实际支付金额为空!");
            return ret__failed;
        }
        double realAmount = Double.parseDouble(orderAmount);
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        //查询订单信息
        boolean orderStatus = getOrderStatus(orderNo);
        if (!orderStatus) {
            logger.info(orderNo + "此订单尚未支付成功！");
            return ret__failed;
        }
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setRet__success(ret__success);
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setIp(ip);
        processNotifyVO.setOrder_no(orderNo);
        processNotifyVO.setTrade_no(tradeNo);
        processNotifyVO.setTrade_status(tradeStatus);
        processNotifyVO.setT_trade_status(tTradeStatus);
        processNotifyVO.setRealAmount(realAmount);
        //回调参数
        processNotifyVO.setInfoMap(JSONObject.fromObject(infoMap).toString());
        processNotifyVO.setPayment("JINY");
        processNotifyVO.setConfig(config);

        return super.processSuccessNotify(processNotifyVO, verifyRequest);
    }

    /**
     * 功能描述:查询订单状态
     *
     * @param orderNo 订单号
     * @return: boolean
     **/
    private boolean getOrderStatus(String orderNo) {
        try {
            //封装请求参数
            Map<String, String> map = new HashMap<>();
//            MerchantNo	String/64	平台分配
            map.put("pay_memberid", merchId);

//            OutTradeNo	String/64	商户订单号
            map.put("pay_orderid", orderNo);

//            签名
            map.put("pay_md5sign", generatorSign(map));

            logger.info("[XYX]逍遥仙支付回调订单查询接口请求参数{}", JSONObject.fromObject(map));
            //发送请求
            String response = HttpUtils.toPostForm(map, queryOrderUrl);

            //解析响应参数
            JSONObject respJson = JSONObject.fromObject(response);
            logger.info("[XYX]逍遥仙支付回调订单查询接口响应信息{}", respJson);
            if (respJson.containsKey("returncode") && "00".equals(respJson.getString("returncode"))) {
                if ("SUCCESS".equalsIgnoreCase(respJson.getString("trade_state"))) {

                    logger.info("[XYX]逍遥仙支付回调订单查询成功,订单" + orderNo + "已支付。");
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XYX]逍遥仙支付回调订单查询异常");
            return false;
        }
    }
}




