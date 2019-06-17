package com.cn.tianxia.api.pay.impl;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
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

public class XFPPayServiceImpl extends PayAbstractBaseService implements PayService{
    private static final Logger logger = LoggerFactory.getLogger(XFPPayServiceImpl.class);
    /**
     * 回调失败响应信息
     */
    private static final String ret__failed = "fail";
    /**
     * 回调成功响应信息
     */
    private static final String ret__success = "OK";
    /**商户号*/
    private String merchId;
    /**秘钥*/
    private String secret;
    /**回调地址*/
    private String notifyUrl;
    /**支付地址*/
    private String payUrl;
    /**订单查询地址*/
    private String queryOrderUrl;
    

    public XFPPayServiceImpl() {
    }

    public XFPPayServiceImpl(Map<String, String> data) {
        if (MapUtils.isNotEmpty(data)) {
            if (data.containsKey("merchId")) {
                this.merchId = data.get("merchId");
            }
            if (data.containsKey("secret")) {
                this.secret = data.get("secret");
            }
            if (data.containsKey("notifyUrl")) {
                this.notifyUrl = data.get("notifyUrl");
            }
            if (data.containsKey("payUrl")) {
                this.payUrl = data.get("payUrl");
            }
            if (data.containsKey("queryOrderUrl")) {
                this.queryOrderUrl = data.get("queryOrderUrl");
            }
        }
    }

	@Override
	public JSONObject wyPay(PayEntity payEntity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject smPay(PayEntity payEntity) {
		try {
			//封装请求参数
			Map<String, String> data = sealRequest(payEntity);
			logger.info("[XFP]讯支付请求参数:"+JSONObject.fromObject(data).toString());
			logger.info("请求地址：{}", payUrl);
			//生成请求表单
			String resStr = HttpUtils.generatorForm(data, payUrl);
			return PayResponse.sm_form(payEntity, resStr, "下单成功");
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("[XFP]讯支付扫码异常:"+e.getMessage());
			return PayResponse.error("[XFP]讯支付扫码异常"+e.getMessage());
		}
	}
	@Override
	public String callback(Map<String, String> data) {
		try {
			data.remove("attach");
            String sourceSign = data.remove("sign");
            logger.info("[XFP]讯支付回调原签名串:"+sourceSign);
            String sign = generatorSign(data);
            logger.info("[XFP]讯支付回调:本地签名:" + sign + "      服务器签名:" + sourceSign);
            if(sign.equals(sourceSign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XFP]讯支付回调验签异常:"+e.getMessage());
        }
        return "fail";
	}
	/**
     * 
     * @Description 封装支付请求参数
     * @param entity
     * @param type 支付类型  1 网银支付   2 扫码支付
     * @return
     * @throws Exception
     */
	public Map<String, String> sealRequest(PayEntity payEntity) throws Exception{
		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD HH:mm:ss");
		String toDate = dateFormat.format(new Date());
		String amount = new DecimalFormat("0.00").format(payEntity.getAmount());
		Map<String,String> data = new HashMap<>();
		data.put("pay_memberid", merchId);
		data.put("pay_orderid", payEntity.getOrderNo());
		data.put("pay_applydate", toDate);
		data.put("pay_bankcode", payEntity.getPayCode());
		data.put("pay_amount", amount);
		data.put("pay_notifyurl", notifyUrl);
		data.put("pay_callbackurl", payEntity.getRefererUrl());
		data.put("pay_md5sign", generatorSign(data));
		return data;
	}
	/**
     * 
     * @Description 生成签名串
     * @param data
     * @return
	 * @throws Exception
     */
    public String generatorSign(Map<String,String> data) throws Exception{
        StringBuffer sb = new StringBuffer();
        Map<String, String> map = new TreeMap<>(data);
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String val = map.get(key);
            if (StringUtils.isBlank(val) || "sign".equalsIgnoreCase(key)) {
                continue;
            }
            sb.append(key).append("=").append(val).append("&");
        }
        sb.append("key=").append(secret);
        //生成待签名串
        String signStr = sb.toString();
        logger.info("[XFP]讯支付生成待签名串:{}",signStr);
        String sign = MD5Utils.md5toUpCase_32Bit(signStr);
        logger.info("[XFP]讯支付生成加密签名串:{}",sign);
        return sign;
    }
    
    /**
     * 订单查询接口
     *
     * @param orderNo
     * @return
     * @Description (TODO这里用一句话描述这个方法的作用)
     */
    public boolean serchOrder(String orderNo) {
    	try {
            Map<String, String> param = new HashMap<>();
            param.put("pay_memberid", merchId);//商户号
            param.put("pay_orderid", orderNo);//商户订单号
            param.put("pay_md5sign", generatorSign(param));//商户订单号
            logger.info("[XFP]讯支付回调查询订单{}请求参数：{}", orderNo, JSONObject.fromObject(param));
            String resStr = HttpUtils.toPostWeb(param, queryOrderUrl);
            logger.info("[XFP]讯支付回调查询订单{}响应信息：{}", orderNo, JSONObject.fromObject(resStr));
            if (StringUtils.isBlank(resStr)) {
                logger.info("[XFP]讯支付回调查询订单发起HTTP请求无响应,订单号{}", orderNo);
                return false;
            }
            JSONObject resJson = JSONObject.fromObject(resStr);
            if (!"00".equals(resJson.getString("returncode"))) {
                return false;
            }
            if (!"SUCCESS".equalsIgnoreCase(resJson.getString("trade_state"))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XFP]讯支付回调查询订单{}异常{}", orderNo, e.getMessage());
            return false;
        }
    }

    /**
     * 回调验签
     *
     * @param data
     * @return
     * @Description (TODO这里用一句话描述这个方法的作用)
     */
    private boolean verifyCallback(Map<String, String> data) {
        try {
            String sourceSign = data.get("sign");
            String sign = generatorSign(data);
            logger.info("[XFP]讯支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            return sourceSign.equalsIgnoreCase(sign);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XFP]讯支付回调生成签名串异常{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        Map<String, String> infoMap = ParamsUtils.getNotifyParams(request);  //获取回调请求参数
        logger.info("[XFP]讯支付回调请求参数：" + JSONObject.fromObject(infoMap));
        if (!MapUtils.isNotEmpty(infoMap)) {
            logger.error("XFPNotify获取回调请求参数为空");
            return ret__failed;
        }
        //参数验签
        this.secret = config.getString("secret");//从配置中获取
        this.merchId = config.getString("merchId");//从配置中获取
        this.queryOrderUrl = config.getString("queryOrderUrl");//从配置中获取

        String order_amount = infoMap.get("amount");//单位：元
        if (StringUtils.isBlank(order_amount)) {
            logger.info("XFPNotify获取实际支付金额为空!");
            return ret__failed;
        }
        double realAmount = Double.parseDouble(order_amount);
        String order_no = infoMap.get("orderid");// 平台订单号
        String trade_no = infoMap.get("transaction_id");// 第三方订单号
        String trade_status = infoMap.get("returncode");
        String t_trade_status = "00";// 表示成功状态

        /**订单查询*/
        if (!serchOrder(order_no)) {
            logger.info("[XFP]讯支付回调查询订单{}失败", order_no);
            return ret__failed;
        }
        /**回调验签*/
        boolean verifyRequest = verifyCallback(infoMap);

        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setRet__success(ret__success);    //成功返回
        processNotifyVO.setRet__failed(ret__failed);      //失败返回
        processNotifyVO.setIp(ip);
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);    //支付状态
        processNotifyVO.setT_trade_status(t_trade_status);     //第三方成功状态
        processNotifyVO.setRealAmount(realAmount);
        processNotifyVO.setInfoMap(JSONObject.fromObject(infoMap).toString());    //回调参数
        processNotifyVO.setPayment("XFP");
        processNotifyVO.setConfig(config);
        return super.processSuccessNotify(processNotifyVO, verifyRequest);
    }
}
