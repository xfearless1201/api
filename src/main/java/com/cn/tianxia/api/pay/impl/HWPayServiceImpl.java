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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @ClassName HWPayServiceImpl
 * @Description 洪武支付支付对接
 * @Author
 * @Date 2019年05月18日 13:51
 * @Version 1.0.0
 **/
public class HWPayServiceImpl extends PayAbstractBaseService implements PayService {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(HWPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private static String ret__success = "SUCCESS";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true


    public HWPayServiceImpl() {
    }

    public HWPayServiceImpl(Map<String, String> data) {
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

        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);
        logger.info("[HW]洪武支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("HWNotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("paysapi_id");//第三方订单号，流水号
        String order_no = dataMap.get("order_id");//支付订单号
        String amount = dataMap.get("real_price");//实际支付金额,以元为单位
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[HW]洪武支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        String trade_status = dataMap.get("code");  //第三方支付状态，1 支付成功
        String t_trade_status = "1";//第三方成功状态

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
        processNotifyVO.setPayment("HW");

        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[HW]洪武支付回调验签失败");
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
		logger.info("[HW]洪武支付扫码支付开始===================START=======================");
        try {
            Map<String,String> map = sealRequest(payEntity);

            String responseData = HttpUtils.generatorForm(map, payUrl);

            if(StringUtils.isBlank(responseData)){
                logger.info("[HW]洪武支付发起HTTP请求无响应");
                return PayResponse.error("[HW]洪武支付扫码支付发起HTTP请求无响应");
            }
            return PayResponse.sm_form(payEntity,responseData,"下单成功");

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[HW]洪武支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[HW]洪武支付扫码支付异常");
        }
	}

	/**
	 * 异步回调接口
	 */
	@Override
	public String callback(Map<String, String> data) {
		logger.info("[HW]洪武支付回调验签开始===================START==============");
        try {
            //获取验签原串
            String sourceSign = data.get("sign");
            //生成待签名串
            String sign = generatorSign(data);
            logger.info("[HW]洪武支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            //验签
            if(sourceSign.equalsIgnoreCase(sign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[HW]洪武支付回调验签异常:"+e.getMessage());
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
            Map<String,String> dataMap = new HashMap<>();

            dataMap.put("return_type", "html");//返回数据类型
            dataMap.put("api_code", merchId);//商户号
            dataMap.put("is_type", entity.getPayCode());//支付类型
            dataMap.put("price",amount);//订单定价
            dataMap.put("order_id", entity.getOrderNo());//您的自定义单号
            dataMap.put("time", String.valueOf(System.currentTimeMillis()/1000));//发起时间
            dataMap.put("mark", "TOP-UP");//描述
            dataMap.put("return_url", entity.getRefererUrl());//成功后网页跳转地址
            dataMap.put("notify_url", notifyUrl);//通知状态异步回调接收地址
            dataMap.put("sign",generatorSign(dataMap));//签名认证串


            logger.info("[HW]洪武支付扫码支付请求参数：{}", JSONObject.fromObject(dataMap));
            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[HW]洪武支付组装支付请求参数异常:"+e.getMessage());
            throw new Exception("[HW]洪武支付组装支付请求参数异常!");
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
        logger.info("[HW]洪武支付生成签名串开始================START=================");
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
            logger.info("[HW]洪武支付扫码支付生成待签名串：{}",strString);
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString());

            logger.info("[HW]洪武支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[HW]洪武支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[HW]洪武支付生成签名串异常!");
        }
    }
}
