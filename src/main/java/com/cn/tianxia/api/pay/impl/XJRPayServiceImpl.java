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
import java.util.*;

/**
 * @ClassName   XJRPayServiceImpl
 * @Description 新金睿支付  渠道:支付宝扫码
 * @Author Vicky
 * @Version 1.0.0
 **/
public class XJRPayServiceImpl extends PayAbstractBaseService implements PayService {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(XJRPayServiceImpl.class);
    private String merchId;//商户id
    private String payUrl;//支付地址
    private String notifyUrl;//回调通知地址
    private String secret;//密钥
    private String queryOrderUrl;

    private static String ret__success = "success";  //成功返回字符串
    private static String ret__failed = "fail";   //失败返回字符串
    private boolean verifySuccess = true;//回调验签默认状态为true

    String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    String noce = UUID.randomUUID().toString().replace("-","");


    public XJRPayServiceImpl() {
    }

    public XJRPayServiceImpl(Map<String, String> data) {
        if(MapUtils.isNotEmpty(data)){
            if(data.containsKey("merchId")){
                this.merchId = data.get("merchId");
            }if(data.containsKey("payUrl")){
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
     */
    @Override
    public String notify(HttpServletRequest request, HttpServletResponse response, JSONObject config) {
        this.merchId = config.getString("merchId");
        this.notifyUrl = config.getString("notifyUrl");
        this.secret = config.getString("secret");
        this.queryOrderUrl = config.getString("queryOrderUrl");

        //商户返回信息
        Map<String, String> dataMap = ParamsUtils.getNotifyParams(request);


        logger.info("[XJR]新金睿支付扫码支付回调请求参数：{}" + JSONObject.fromObject(dataMap));

        if (!MapUtils.isNotEmpty(dataMap)) {
            logger.error("XJRNotify获取回调请求参数为空");
            return ret__failed;
        }

        String trade_no = dataMap.get("channelOrderNo");//第三方订单号，流水号
        String order_no = dataMap.get("orderNo");//支付订单号
        String amount = dataMap.get("amount");//终端支付用户实际支付金额，依据此金额上分
        String ip = StringUtils.isBlank(IPTools.getIp(request)) ? "127.0.0.1" : IPTools.getIp(request);

        if (StringUtils.isBlank(trade_no)) {
            logger.info("[XJR]新金睿支付扫码支付回调请求参数,获取的{} 流水单号为空",trade_no);
            return ret__failed;
        }

        //0未处理、2处理失败、3处理中、6处理成功
        String trade_status = dataMap.get("transState");  //第三方支付状态
        String t_trade_status = "6";//第三方成功状态

        /**订单查询*/
        if(!queryOrder(order_no)) {
            logger.info("[[XJR]新金睿支付扫码支付回调查询订单{}失败", order_no);
            return ret__failed;
        }

        //写入数据库
        ProcessNotifyVO processNotifyVO = new ProcessNotifyVO();
        processNotifyVO.setOrder_no(order_no);
        processNotifyVO.setRealAmount(Double.parseDouble(amount)/100);//以“分”为单位
        processNotifyVO.setIp(ip);
        processNotifyVO.setTrade_no(trade_no);
        processNotifyVO.setTrade_status(trade_status);
        processNotifyVO.setRet__failed(ret__failed);
        processNotifyVO.setRet__success(ret__success);
        processNotifyVO.setInfoMap(JSONObject.fromObject(dataMap).toString());
        processNotifyVO.setT_trade_status(t_trade_status);
        processNotifyVO.setConfig(config);
        processNotifyVO.setPayment("XJR");



        //回调验签
        if ("fail".equals(callback(dataMap))) {
            verifySuccess = false;
            logger.info("[XJR]新金睿支付回调验签失败");
            return ret__failed;
        }
        return processSuccessNotify(processNotifyVO, verifySuccess);
    }

    private  Boolean queryOrder(String order_no){
        //订单查询
        try{

            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("merchNo", merchId);//
            queryMap.put("reqType","20002");//
            queryMap.put("orderNo",order_no);//
           // queryMap.put("channelOrderNo","");//渠道订单号
            queryMap.put("orderTime", time);//
            queryMap.put("randomStr", noce);//
            queryMap.put("signType ","MD5");//
            queryMap.put("sign", generatorSign(queryMap));//

            logger.info("[XJR]新金睿支付扫码支付回调查询订单{}请求参数：{}", order_no, JSONObject.fromObject(queryMap));

            String ponse =HttpUtils.toPostForm(queryMap, queryOrderUrl);

            if(StringUtils.isBlank(ponse)){
                logger.info("[XJR]新金睿支付回调查询订单发起HTTP请求无响应");
                return false;
            }
            logger.info("[XJR]新金睿支付扫码支付回调查询订单{}响应信息：{}", order_no, ponse);

            JSONObject jb = JSONObject.fromObject(ponse);

            if(jb.containsKey("respCode") && "0000".equalsIgnoreCase(jb.getString("respCode"))){
                //交易状态 0未处理、2处理失败、3处理中、6处理成功,
                  if("3".equals(jb.getString("transState")) || "6".equals(jb.getString("transState"))){
                      return true;
                  }else {
                      logger.info("[XJR]新金睿支付扫码支付回调查询订单{}交易状态：{}", order_no, jb.getString("transState"));
                      return false;
                  }
            }else {
                logger.info("[XJR]新金睿支付扫码支付回调查询订单{}响应描述：{}", order_no, jb.getString("respMsg"));
                return false;
            }

        }catch (Exception e){
            e.getStackTrace();
            logger.info("[XJR]新金睿支付扫码支付回调查询订单{}异常{}：",order_no,e.getMessage());
            return false;
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
		logger.info("[XJR]新金睿支付扫码支付开始===================START=======================");
        try {
            Map<String,String> map = sealRequest(payEntity);
            logger.info("[XJR]新金睿支付扫码支付请求参数：{}", JSONObject.fromObject(map));

            String responseData = HttpUtils.toPostForm(map, payUrl);
            logger.info("[XJR]新金睿支付扫码支付HTTP响应参数：{}", JSONObject.fromObject(responseData));

            JSONObject jb = JSONObject.fromObject(responseData);
            if(jb.containsKey("respCode") && "0000".equalsIgnoreCase(jb.getString("respCode"))){

                if(StringUtils.isBlank(payEntity.getMobile())){
                    return PayResponse.sm_qrcode(payEntity,jb.getString("payUrl"),"下单成功");
                }else {
                    return PayResponse.sm_link(payEntity,jb.getString("payUrl"),"下单成功");
                }

            }
            return PayResponse.error("下单失败：" + jb.get("respMsg"));

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XJR]新金睿支付扫码支付异常:{}", e.getMessage());
            return PayResponse.error("[XJR]新金睿支付扫码支付异常");
        }
	}


    public String callback(Map<String, String> data) {
        logger.info("[XJR]新金睿支付回调验签开始===================START==============");
        try {
            //获取验签原串
            String sourceSign = data.remove("sign");
            //生成待签名串
            String sign = generatorSign(data);
            logger.info("[XJR]新金睿支付扫码支付生成签名串：{}--源签名串：{}", sign, sourceSign);
            //验签
            if(sourceSign.equalsIgnoreCase(sign)) return "success";
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[XJR]新金睿支付回调验签异常:"+e.getMessage());
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
        logger.info("[XJR]新金睿支付组装支付请求参数开始===================START==================");
        try {

            String amount = new DecimalFormat("##").format(entity.getAmount() *100);
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            Map<String,String> dataMap = new HashMap<>();
            dataMap.put("merchNo", merchId);//商户号，由平台服务商开户提供
            dataMap.put("reqType","20001");//接口类型：20001
            dataMap.put("transType", entity.getPayCode());//请求交易类型：
            dataMap.put("orderNo", entity.getOrderNo());//订单号
            dataMap.put("orderTime",time);//订单时间格式 20190228111230
            dataMap.put("amount", amount);//交易金额 分 单位
           // dataMap.put("cardNo","");//付款卡号、部分交易类型需要上送
           // dataMap.put("accountName","");//付款账户名、部分交易类型需要上送
            dataMap.put("notifyUrl", notifyUrl);//交易通知地址
            //dataMap.put("redirectUrl", entity.getRefererUrl());//页面跳转地址
            //dataMap.put("orderTitle", "top-up");//订单标题
           // dataMap.put("orderDesc","recharge");//订单描述
           // dataMap.put("reqIp", entity.getIp());//部分类型需要上送
            dataMap.put("randomStr", noce);//随机字符串
            dataMap.put("signType","MD5");//默认值 MD5
            dataMap.put("sign",generatorSign(dataMap));//MD5签名

            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[XJR]新金睿支付组装支付请求参数异常:"+e.getMessage());
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
	private String generatorSign(Map<String,String> data ) throws Exception{
        logger.info("[XJR]新金睿支付生成签名串开始================START=================");
        try {
            TreeMap<String,String> treeMap = new TreeMap<>(data);
            StringBuffer sb = new StringBuffer();
            Iterator<String> iterator = treeMap.keySet().iterator();
            while (iterator.hasNext()){
                String key = iterator.next();
                String val = treeMap.get(key);
                if(StringUtils.isBlank(val) || "sign".equalsIgnoreCase(key)){
                    continue;
                }
                sb.append(key).append("=").append(val).append("&");
            }
            sb.append("key=").append(secret);
            logger.info("[XJR]新金睿支付扫码支付生成待签名串：{}",sb.toString());
            String sign = MD5Utils.md5toUpCase_32Bit(sb.toString());

            logger.info("[XJR]新金睿支付扫码支付生成签名串：{}",sign);
            return sign;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[XJR]新金睿支付扫码支付生成签名串异常：{}",e.getMessage());
            throw new Exception("[XJR]新金睿支付生成签名串异常!");
        }
    }



}
