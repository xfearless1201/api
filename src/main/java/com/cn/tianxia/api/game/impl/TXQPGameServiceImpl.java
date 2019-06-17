package com.cn.tianxia.api.game.impl;

import com.alibaba.fastjson.JSONObject;
import com.cn.tianxia.api.utils.Encrypt;
import com.cn.tianxia.api.utils.PlatFromConfig;
import com.cn.tianxia.api.utils.v2.MD5Utils;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Jacky
 * @version 1.0.0
 * @ClassName TXQPGameServiceImpl
 * @Description TXQP 天下棋牌
 * @Date 2019年5月30日 下午18:31:46
 */
public class TXQPGameServiceImpl {

    private final static Logger logger = LoggerFactory.getLogger(TXQPGameServiceImpl.class);
    private static final String PARAM_AGENT = "agentid=";
    private static final String PARAM_TIMESTAMP = "&timestamp=";
    private static final String PARAM_PARAM = "&paraValue=";
    private static final String PARAM_TYPE = "&type=";
    private static final String PARAM_KEY = "&key=";
    private static final String ERROR = "error";
    private static final String SUCCESS = "success";
    private static final String FAILED = "failed";
    private static final String PROCESS = "process";
    private static StringBuilder stringBuilder;
    private static String api_url;
    private static String api_deskey;
    private static String api_md5key;
    private static String api_cagent;
    private static String lineCode;
    private static final String PC_MODEL = "0";
    private static final String H5_MODEL = "1";
    private static final String ANDROID_MODEL = "MB";
    private static final String IOS_MODEL = "MB";


    static {
        stringBuilder = new StringBuilder(50);
    }

    public TXQPGameServiceImpl(Map<String, String> map) {
        PlatFromConfig pf = new PlatFromConfig();
        pf.InitData(map, "TXQP");
        net.sf.json.JSONObject jo = net.sf.json.JSONObject.fromObject(pf.getPlatform_config());
        if (jo != null && !jo.isEmpty()) {
            if (jo.containsKey("api_url")) {
                api_url = jo.getString("api_url");
            }
            if (jo.containsKey("api_deskey")) {
                api_deskey = jo.getString("api_deskey");
            }
            if (jo.containsKey("api_md5key")) {
                api_md5key = jo.getString("api_md5key");
            }
            if (jo.containsKey("api_cagent")) {
                api_cagent = jo.getString("api_cagent");
            }
            if (jo.containsKey("lineCode")) {
                lineCode = jo.getString("lineCode");
            }
        }
    }


    public String loginGame(GameForwardVO gameForwardVO) throws Exception {
        logger.info("loginGame(GameForwardVO gameForwardVO =  -start {}", gameForwardVO);
        if (StringUtils.isEmpty(gameForwardVO.getAg_username()) ||
                StringUtils.isEmpty(gameForwardVO.getIp())) {
            throw new Exception("[TXQP]天下棋牌 发起HTTP登录请求参数不足！");
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        stringBuilder = new StringBuilder()
                .append("account=").append(gameForwardVO.getAg_username())
                .append("&linecode=").append(lineCode)
                .append("&nickname=").append(gameForwardVO.getAg_username())
                .append("&lastloginip=").append(gameForwardVO.getIp());
        logger.info("[TXQP] 创建用户代加密参数串：{}", stringBuilder.toString());
        String responseString = sendGet(setParam("0", stringBuilder.toString(), timestamp).toString());
        String url = ERROR;
        JSONObject responseJson = JSONObject.parseObject(responseString);
        if (responseJson.containsKey("data")) {
            JSONObject jsonData = JSONObject.parseObject(responseJson.getString("data"));
            if ("0".equals(jsonData.getString("code"))) {
                url = jsonData.getString("url");
                String loginType = null;
                String gameId = StringUtils.isEmpty(gameForwardVO.getGameId()) ? "0" : gameForwardVO.getGameId();
                //PC端：0     H5：1        安卓：2        IOS：4       原生APP端：3
                if (PC_MODEL.equals(gameForwardVO.getModel())) {
                    loginType = "1";
                }
                if (ANDROID_MODEL.equals(gameForwardVO.getModel())) {
                    loginType = "2";
                }
                if (H5_MODEL.equals(gameForwardVO.getModel())) {
                    loginType = "1";
                }
                if (IOS_MODEL.equals(gameForwardVO.getModel())) {
                    loginType = "4";
                }
                if (loginType != null && !"0".equals(gameId)) {
                    url += "&logintype=" + loginType + "&gameid=" + gameId;
                }
            }
        }
        logger.info("[TXQP]天下棋牌发起HTTP请求URL：{}", url);
        return url;
    }


    public String transferIn(GameTransferVO gameTransferVO) throws Exception {
        return transfer(gameTransferVO, "2");
    }


    public String transferOut(GameTransferVO gameTransferVO) throws Exception {
        return transfer(gameTransferVO, "3");
    }


    public String transfer(GameTransferVO gameTransferVO, String type) throws Exception {

        logger.info("transfer(GameTransferVO gameTransferVO = {},String type = {}", gameTransferVO, type);
        if (StringUtils.isEmpty(gameTransferVO.getAg_username()) ||
                StringUtils.isEmpty(gameTransferVO.getBillno()) ||
                StringUtils.isEmpty(gameTransferVO.getMoney())) {
            throw new Exception("[TXQP]天下棋牌  发起HTTP转账请求参数不足！");
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        stringBuilder = new StringBuilder()
                .append("account=").append(gameTransferVO.getAg_username())
                .append("&score=").append(gameTransferVO.getMoney())
                .append("&orderid=").append(gameTransferVO.getBillno());
        logger.info("[TXQP]天下棋牌 待加密参数串：{}", stringBuilder.toString());

        String responseString = sendGet(setParam(type, stringBuilder.toString(), timestamp).toString());

        if (responseString != null) {
            if (responseString.equals(ERROR)) {
                return ERROR;
            }
            JSONObject responseJson = JSONObject.parseObject(responseString);
            if (responseJson.containsKey("data")) {
                JSONObject jsonData = JSONObject.parseObject(responseJson.get("data").toString());
                if ("0".equals(jsonData.getString("code"))) {
                    return loopOrder(gameTransferVO.getAg_username(), gameTransferVO.getBillno());
                }
            }
        }
        return loopOrder(gameTransferVO.getAg_username(), gameTransferVO.getBillno());
    }


    public String getBalance(String gamename) throws Exception {

        logger.info("getBalance(String userName = -start {}", gamename);
        if (StringUtils.isEmpty(gamename)) {
            throw new Exception("[TXQP]天下棋牌  发起HTTP查询余额请求参数不足！");
        }
        String responseString = sendGet(setParam("1", "account=" + gamename,
                String.valueOf(System.currentTimeMillis())).toString());
        String balance = ERROR;
        JSONObject responseJson = JSONObject.parseObject(responseString);
        if (responseJson.containsKey("data")) {
            JSONObject jsonData = JSONObject.parseObject(responseJson.get("data").toString());
            if ("0".equals(jsonData.getString("code"))) {
                balance = jsonData.getString("score");
            }
        }
        return balance;
    }


    private String getOrderSattus(String userName, String orderNo) throws Exception {

        logger.info("getOrderSattus(String userName = {} ,String orderNo = {})");
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(orderNo)) {
            throw new Exception("[TXQP]天下棋牌  发起HTTP查询订单请求参数不足！");
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        stringBuilder = new StringBuilder()
                .append("account=").append(userName)
                .append("&orderid=").append(orderNo);
        logger.info("[TXQP]天下棋牌 待加密参数串：{}", stringBuilder.toString());
        String responseString = sendGet(setParam("4", stringBuilder.toString(), timestamp).toString());
        if (responseString == null || responseString.equals(ERROR)) {
            return responseString;
        }
        JSONObject responseJson = JSONObject.parseObject(responseString);
        if (responseJson.containsKey("data")) {
            JSONObject jsonData = JSONObject.parseObject(responseJson.getString("data"));
            if ("1".equals(jsonData.getString("status"))) {
                return SUCCESS;
            }
        }

        return PROCESS;

    }


    private String loopOrder(String userName, String orderNo) throws Exception {

        if (getOrderSattus(userName, orderNo).equals(SUCCESS)) {
            return SUCCESS;
        }
        int i = 0;
        while (i < 2) {
            i++;
            Thread.sleep(3000);
            if (getOrderSattus(userName, orderNo).equals(SUCCESS)) {
                return SUCCESS;
            }
        }
        return ERROR;
    }


    private StringBuilder setParam(String type, String param, String timestamp) throws Exception {
        logger.info("出事话请求参数:类型[" + type + "],参数[" + param + "],时间戳:[" + timestamp + "]");
        return stringBuilder = new StringBuilder(api_url)
                .append(PARAM_AGENT).append(api_cagent)
                .append(PARAM_TIMESTAMP).append(timestamp)
                .append(PARAM_TYPE).append(type)
                .append(PARAM_PARAM).append(Encrypt.AESEncrypt(param, api_deskey))
                .append(PARAM_KEY).append(MD5Utils.md5toUpCase_32Bit(api_cagent + timestamp + api_md5key));
    }

    public String sendGet(String tagUrl) {
        logger.info("sendGet(String tagUrl  = {} -start", tagUrl);
        HttpClient client = new HttpClient();
        GetMethod myGet = new GetMethod(tagUrl);
        String responseString = null;
        try {
            myGet.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            myGet.setRequestHeader("charset", "utf-8");
            int statusCode = client.executeMethod(myGet);
            if (statusCode == HttpStatus.SC_OK) {
                InputStream inputStream = myGet.getResponseBodyAsStream();
                responseString = IOUtils.toString(inputStream, Consts.UTF_8);
                logger.info("[TXQP]天下棋牌 HTTP请求握手成功！响应报文：{}", JSONObject.parseObject(responseString));
            } else {
                logger.info("[TXQP]天下棋牌 发起HTTP请求握手失败！失败状态码: {}", statusCode);
                if (statusCode > 300) {
                    responseString = ERROR;
                }
            }
        } catch (IOException e) {
            logger.error("[TXQP]天下棋牌 HTTP请求Time Out", e);
            if (!"Read timed out".equals(e.getMessage())) {
                responseString = ERROR;
            }
        } catch (Exception e) {
            logger.error("[TXQP]天下棋牌 发起HTTP请求错误！", e);
            throw e;
        } finally {
            myGet.releaseConnection();
            client.getHttpConnectionManager().closeIdleConnections(0);
        }
        return responseString;
    }

}
