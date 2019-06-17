/****************************************************************** 
 *
 * Powered By tianxia-online. 
 *
 * Copyright (c) 2018-2020 Digital Telemedia 天下网络 
 * http://www.d-telemedia.com/ 
 *
 * Package: com.cn.tianxia.pay.impl 
 *
 * Filename: CORALPayServiceImpl.java
 *
 * Description: BL宝来支付对接
 *
 * Copyright: Copyright (c) 2018-2020 
 *
 * Company: 天下网络科技 
 *
 * @author: Kay
 *
 * @version: 1.0.0
 *
 * Create at: 2018年10月11日 20:51
 *
 * Revision: 
 *
 * 2018/10/11 20:51
 * - first revision 
 *
 *****************************************************************/
package com.cn.tianxia.api.pay.impl;


import com.cn.tianxia.api.common.PayEntity;
import com.cn.tianxia.api.domain.txdata.v2.RechargeDao;
import com.cn.tianxia.api.pay.PayAbstractBaseService;
import com.cn.tianxia.api.pay.PayService;
import com.cn.tianxia.api.po.pay.PayResponse;
import com.cn.tianxia.api.project.v2.RechargeEntity;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.SpringContextUtils;
import com.cn.tianxia.api.utils.pay.HttpUtils;
import com.cn.tianxia.api.utils.pay.JHNFUtils;
import com.cn.tianxia.api.utils.pay.MapUtils;
import com.cn.tianxia.api.utils.pay.ParamsUtils;
import com.cn.tianxia.api.utils.pay.MD5Utils;
import com.cn.tianxia.api.vo.ProcessNotifyVO;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ClassName JHYPayServiceImpl
 * @Description 聚合翼支付
 * @Author VICKY
 * @Date 2019年05月18日 17:51 对接支付宝渠道； 2019年05月29日新微信渠道
 * @Version 1.0.0
 **/
public class JHYPayServiceImpl extends PayAbstractBaseService implements PayService {
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(JHYPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String robin;//轮训
    private String thoroughfare;
    private String keyId;
    private String queryOrderUrl;

    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true


    public JHYPayServiceImpl() {
    }

    public JHYPayServiceImpl(Map<String, String> data,  String type) {
        if(MapUtils.isNotEmpty(data)){
            if(data.containsKey(type)) {
                JSONObject jsonObject = JSONObject.fromObject(data.get(type));
                if (jsonObject.containsKey("merchId")) {
                    this.merchId = jsonObject.getString("merchId");
                }
                if (jsonObject.containsKey("payUrl")) {
                    this.payUrl = jsonObject.getString("payUrl");
                }
                if (jsonObject.containsKey("notifyUrl")) {
                    this.notifyUrl = jsonObject.getString("notifyUrl");
                }
                if (jsonObject.containsKey("secret")) {
                    this.secret = jsonObject.getString("secret");
                }
                if (jsonObject.containsKey("robin")) {
                    this.robin = jsonObject.getString("robin");
                }
                if (jsonObject.containsKey("keyId")) {
                    this.keyId = jsonObject.getString("keyId");
                }
                if (jsonObject.containsKey("thoroughfare")) {
                    this.thoroughfare = jsonObject.getString("thoroughfare");
                }
                if (jsonObject.containsKey("queryOrderUrl")) {
                    this.queryOrderUrl = jsonObject.getString("queryOrderUrl");
                }
            }
        }
    }
    /**
     * 回调
     */
    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        //解析
        RechargeDao rechargeDao = (RechargeDao) SpringContextUtils.getBeanByClass(RechargeDao.class);
        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);
        logger.info("[JHY]聚合翼支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("JHYNotify获取回调请求参数为空");
            return ret__failed;
        }
        String trade_no = null;
        String order_no = null;
        String amount = null;
        String trade_status = null;
        String t_trade_status = null;
        //区分支付宝、微信
        if(dataMap.containsKey("transaction_id")){
            logger.info("[JHY]聚合翼支付====== 微信回调 ======");
            //微信
            trade_no = dataMap.get("transaction_id");//第三方订单号，流水号
            order_no = dataMap.get("orderid");//支付订单号
            amount = dataMap.get("amount");//实际支付金额,以分为单位
            trade_status = dataMap.get("returncode");  //第三方支付状态，1 支付成功
            t_trade_status = "00";//第三方成功状态
        }else if(dataMap.containsKey("trade_no")){
            logger.info("[JHY]聚合翼支付====== 支付宝回调 ======");
            trade_no = dataMap.get("trade_no");//第三方订单号，流水号
            order_no = dataMap.get("out_trade_no");//支付订单号
            amount = dataMap.get("amount");//实际支付金额,以元为单位
            trade_status = dataMap.get("status");  //第三方支付状态，1 支付成功
            t_trade_status = "success";//第三方成功状态
        }
        if (StringUtils.isBlank(trade_no)) {
            logger.info("[JHY]聚合翼微信扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }
        if (StringUtils.isBlank(amount) || amount == null) {
            logger.info("[JHY]聚合翼微信扫码支付回调请求参数,订单金额为空");
            return ret__failed;
        }

        //写入数据库
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setRealAmount(Double.parseDouble(amount));
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);
        processNotifyVO.setT_trade_status(t_trade_status);
        processNotifyVO.setInfoMap(JSONObject.fromObject(dataMap).toString());
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setConfig(config);
        processNotifyVO.setPayment("JHY");
        processNotifyVO.setIp(ip);

        RechargeEntity rechargeEntity = rechargeDao.selectByOrderNo(order_no);
        String type = getPayConfigType(String.valueOf(rechargeEntity.getPayType()));//获取支付类型
        config = config.getJSONObject(type);

        String flag = null;

        if(dataMap.containsKey("transaction_id")){

            this.merchId = config.getString("merchId");
            this.notifyUrl = config.getString("notifyUrl");
            this.secret = config.getString("secret");
            this.queryOrderUrl = config.getString("queryOrderUrl");
            //微信，订单查询正确返回OK
            processNotifyVO.setRet__success(queryOrder(order_no, config));//成功返回字符串

            flag = callback(dataMap);
        }else {
            //支付宝
            this.merchId = config.getString("merchId");
            this.notifyUrl = config.getString("notifyUrl");
            this.secret = config.getString("secret");
            this.robin = config.getString("robin");
            this.keyId = config.getString("keyId");
            this.thoroughfare = config.getString("thoroughfare");

            processNotifyVO.setRet__success("success");//成功返回字符串

            flag = callback(dataMap);
        }

        //回调验签
        if ("fail".equals(flag)) {
            verifySuccess = false;
            logger.info("[JHY]聚合翼支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    /**
     * 微信支付    订单查询
     * @param order
     * @param config
     * @return
     */
    private String queryOrder(String order, JSONObject config){

        this.merchId = config.getString("merchId");
        this.notifyUrl = config.getString("notifyUrl");
        this.secret = config.getString("secret");
        this.queryOrderUrl = config.getString("queryOrderUrl");

        try{
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("pay_memberid", merchId);
            queryMap.put("pay_orderid", order);
            queryMap.put("pay_md5sign", generatorSign(queryMap, 2));

            logger.info("[JHYT]聚合翼支付微信扫码支付回调查询订单{}请求参数：{}", order, JSONObject.fromObject(queryMap));
            String ponse = HttpUtils.toPostForm(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[JHYT]聚合翼支付微信回调查询订单发起HTTP请求无响应");
                return ret__failed;
            }
            logger.info("[JHYT]聚合翼支付微信扫码支付回调查询订单{}响应信息：{}", order, JSONObject.fromObject(ponse));

            JSONObject jb = JSONObject.fromObject(ponse);
            if(jb.containsKey("returncode") && "00".equals(jb.getString("returncode"))){

                if("NOTPAY".equals(jb.getString("trade_state"))){
                    logger.info("[JHYT]聚合翼支付微信扫码支付回调查询订单,订单支付状态为:{}",jb.getString("trade_state"));
                    return ret__failed ;
                }else {
                    return "OK";
                }
            }else {
                logger.info("[JHYT]聚合翼支付微信扫码支付回调查询订单,请求状态为:{}",jb.getString("returncode"));
                return ret__failed;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[JHYT]聚合翼支付微信扫码支付回调查询订单{}异常{}：",order,e.getMessage());
            return ret__failed;
        }
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
        logger.info("[JHY]聚合翼支付扫码支付开始===================START=======================");
        try {
            Map<String,String> map = null;

            if("2".equalsIgnoreCase(payEntity.getPayCode())){
                //支付宝
                map = sealRequest(payEntity, 1);
                logger.info("[JHY]聚合翼支付宝扫码支付请求参数：{}", JSONObject.fromObject(map));
            }else {
                map = sealRequest(payEntity, 2);
                logger.info("[JHY]聚合翼微信扫码支付请求参数：{}", JSONObject.fromObject(map));
            }

            String responseData = HttpUtils.generatorForm(map, payUrl);

            if(StringUtils.isBlank(responseData)){
                logger.info("[JHY]聚合翼支付发起HTTP请求无响应");
                return PayResponse.error("[JHY]聚合翼支付扫码支付发起HTTP请求无响应");
            }
            return PayResponse.sm_form(payEntity,responseData,"下单成功");

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[JHY]聚合翼支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[JHY]聚合翼支付扫码支付异常");
        }
    }

    /**
     * 异步回调接口
     */
    @Override
    public String callback(Map<String, String> data) {
        logger.info("[JHY]聚合翼支付回调验签开始===================START==============");
        try {
            //获取验签原串
            String sourceSign = data.get("sign");
            //生成待签名串
            String sign =null;
            if(data.containsKey("transaction_id")){
                //微信
                sign = generatorSign(data,2);
            }else {
                sign = generatorSign(data,1);
            }

            logger.info("[JHY]聚合翼支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            //验签
            if(sourceSign.equalsIgnoreCase(sign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[JHY]聚合翼支付回调验签异常:"+e.getMessage());
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
    private Map<String,String> sealRequest(PayEntity entity, Integer type) throws Exception{
        logger.info("[JHY]聚合翼支付组装支付请求参数开始===================START==================");
        try {

            String amount = new DecimalFormat("0.00").format(entity.getAmount());
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            //创建存储支付请求参数对象
            Map<String,String> dataMap = new HashMap<>();
            //支付宝参数
            if(1 == type){
                dataMap.put("account_id", merchId);//商户ID、在平台首页右边获取商户ID
                dataMap.put("content_type", "text");//请求过程中返回的网页类型，text或json机
                dataMap.put("thoroughfare", thoroughfare);//初始化支付通道，目前通道：wechat_auto（公开版微信）、alipay_auto（公开版支付宝）、service_auto（服务版微信/支付宝）
                dataMap.put("type", entity.getPayCode());//是string支付类型，该参数在服务版下有效（service_auto），其他可为空参数，微信：1，支付宝：21
                dataMap.put("out_trade_no", entity.getOrderNo());//订单信息，在发起订单时附加的信息，如用户名，充值订单号等字段参数
                dataMap.put("robin", robin);//轮训，2：开启轮训，1：进入单通道模式
                dataMap.put("keyId",    keyId);//设备KEY，在公开版列表里面Important参数下的DEVICE Key一项，如果该请求为轮训模式，则本参数无效，本参数为单通道模式
                dataMap.put("amount", amount);//支付金额，在发起时用户填写的支付金额
                dataMap.put("callback_url", notifyUrl);//异步通知地址，在支付完成时，本平台服务器系统会自动向该地址发起一条支付成功的回调请求,
                dataMap.put("success_url", entity.getRefererUrl());//支付成功后网页自动跳转地址，仅在网页类型为text下有效，json会将该参数返回
                dataMap.put("error_url", entity.getRefererUrl());//支付失败时，或支付超时后网页自动跳转地址，仅在网页类型为text下有效，json会将该参数返回
                dataMap.put("sign", generatorSign(dataMap, 1));//签名算法，在支付时进行签名算法，详见《聚合翼支付签名算法》
            }else {
                //微信参数
                dataMap.put("pay_memberid", merchId);//商户号，平台分配商户号
                dataMap.put("pay_orderid", entity.getOrderNo());//订单号，上送订单号唯一, 字符长度20
                dataMap.put("pay_applydate", time);//提交时间，时间格式：2016-12-26 18:18:18
                dataMap.put("pay_bankcode", entity.getPayCode());//银行编码，在商户中心查询
                dataMap.put("pay_notifyurl", notifyUrl);//服务端通知，服务端返回地址.（POST返回数据）
                dataMap.put("pay_callbackurl", entity.getRefererUrl());//页面跳转通知，页面跳转返回地址（POST返回数据）
                dataMap.put("pay_amount", amount);//订单金额，单位：元
                dataMap.put("pay_md5sign", generatorSign(dataMap,2));//MD5签名，请查看签名算法
                dataMap.put("pay_productname", "TOP-UP");//商品名称
            }


            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[JHY]聚合翼支付组装支付请求参数异常:"+e.getMessage());
            throw new Exception("[JHY]聚合翼支付组装请求参数异常!");
        }
    }


    /**
     *
     * @Description 生成签名串
     * @param data
     * @return
     * @throws Exception
     */
    private String generatorSign(Map<String,String> data, Integer type) throws Exception{
        logger.info("[JHY]聚合翼支付生成签名串开始================START=================");
        try {

            StringBuilder sb = new StringBuilder();
            String sign = null;
            if(1 == type){
                //支付宝加密串
                sb.append(data.get("amount")).append(data.get("out_trade_no"));
                String signStr = sb.toString();

                logger.info("[JHY]聚合翼支付宝生待签名串:{}",signStr);
                String md5tolowerStr = MD5Utils.md5toUpCase_32Bit(signStr).toLowerCase();
                logger.info("[JHY]聚合翼支付宝扫码支付生成第一步待加密串:{}",md5tolowerStr);
                byte[] rc4bytes = JHNFUtils.encry_RC4_byte(md5tolowerStr,secret);
                sign = MD5Utils.md5(rc4bytes).toLowerCase();

            }else {
                //微信
                Map<String, String> treeMap = new TreeMap<>(data);
                Iterator<String> iterator = treeMap.keySet().iterator();
                while (iterator.hasNext()){
                    String key = iterator.next();
                    String val = treeMap.get(key);
                    if(StringUtils.isBlank(val) || "sign".equals(key) || "pay_md5sign".equals(key)){
                        continue;
                    }
                    sb.append(key).append("=").append(val).append("&");
                }
                sb.append("key=").append(secret);

                String strString = sb.toString();
                logger.info("[JHY]聚合翼微信扫码支付生成待签名串：{}",strString);
                sign = MD5Utils.md5toUpCase_32Bit(sb.toString());

            }

            logger.info("[JHY]聚合翼支付扫码支付生成签名串：{}",sign);
            return sign;

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[JHY]聚合翼支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[JHY]聚合翼支付生成签名串异常!");
        }
    }
}
