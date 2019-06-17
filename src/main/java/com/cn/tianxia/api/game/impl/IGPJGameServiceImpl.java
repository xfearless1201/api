package com.cn.tianxia.api.game.impl;

import com.cn.tianxia.api.utils.DESEncrypt;
import com.cn.tianxia.api.utils.FileLog;
import com.cn.tianxia.api.utils.PlatFromConfig;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Hardy
 * @version 1.0.0
 * @ClassName IGPJServiceImpl
 * @Description IG新彩
 * @Date 2019年2月9日 下午4:31:46
 */
public class IGPJGameServiceImpl {
    private static final String ERROR = "error";
    private static Logger logger = LoggerFactory.getLogger(IGPJGameServiceImpl.class);
    private static String apiurl;// 命令(3) (4) (5) (6)对接地址为
    private static String hashcode;
    private static String line;
    private static String lotto_url; // 香港彩 命令(1) (2) 对接地址为
    private static String lottery_url; // 时时彩 命令(1) (2) 对接地址为
    private static String gfc_url; // 官方彩

    private static String currency;// 游戏货币类型

    private static String mobileVersion; // 彩票版本
    DESEncrypt d = new DESEncrypt("");

    public IGPJGameServiceImpl(Map<String, String> pmap, String cagent) {
        PlatFromConfig pf = new PlatFromConfig();
        pf.InitData(pmap, "IGPJ");
        JSONObject jsonObject = JSONObject.fromObject(pf.getPlatform_config());
        JSONObject jo;
        if (jsonObject.containsKey(cagent)) {
            jo = JSONObject.fromObject(jsonObject.getString(cagent));
        } else if (jsonObject.containsKey("ALL")) {
            jo = JSONObject.fromObject(jsonObject.getString("ALL"));
        } else {
            jo = jsonObject;
        }
        apiurl = jo.getString("apiurl");
        hashcode = jo.getString("hashcode");
        lotto_url = jo.getString("lotto_url");
        lottery_url = jo.getString("lottery_url");
        currency = jo.getString("currency");
        gfc_url = jo.getString("gfc_url");
        try {
            line = jo.getString("line");
        } catch (Exception e) {
            line = "1";
        }

        if (jo.containsKey("mobileVersion")) {
            mobileVersion = jo.getString("mobileVersion");
        } else {
            mobileVersion = "new";
        }
    }



    public String loginGame(String username, String password, String gameType, String gameId, String type,
                            String handicap) {
        logger.info("用户【" + username + "】调用IGPJ游戏登录业务开始");
        password = DESEncrypt.getMd5(password);

        // TODO ig埔京测试帐号的游戏币种类型:TEST

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", username);
        jsonObject.put("password", password);
        jsonObject.put("currency", currency);
        jsonObject.put("language", "CN");
        jsonObject.put("gameType", gameType);
        jsonObject.put("line", Integer.parseInt(line));
        jsonObject.put("userCode", username);
        jsonObject.put("nickname", username);

        if ("LOTTERY".equals(gameType)) {
            jsonObject.put("lotteryTray", handicap);
            jsonObject.put("lotteryPage", gameId);
            jsonObject.put("lotteryType", type);
            jsonObject.put("mobileVersion", mobileVersion);
        }
        if ("LOTTO".equals(gameType)) {
            jsonObject.put("lottoTray", handicap);
            jsonObject.put("lottoType", type);
            jsonObject.put("mobileVersion", mobileVersion);
        }
        if ("GFC".equals(gameType)){
            jsonObject.put("gfcTray",handicap);
            jsonObject.put("gfcType",type);
            jsonObject.put("gfcPage",gameId);
        }
        String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"LOGIN\",\"params\":" + jsonObject + "}";
        logger.info("用户【" + username + "】调用IGPJ游戏登录业务,请求参数:{}", data);
        // 香港彩票
        if ("LOTTO".equals(gameType)) {
            apiurl = lotto_url;
            // 时时彩
        } else if ("LOTTERY".equals(gameType)) {
            apiurl = lottery_url;
        }else if ("GFC".equals(gameType)){
            //官方彩
            apiurl = gfc_url;
        }
        logger.info("用户【" + username + "】调用IGPJ游戏登录业务,请求地址:{}", apiurl);
        String msg = sendPost(apiurl, data);
        if (StringUtils.isBlank(msg)) {
            logger.error("用户【" + username + "】调用IGPJ游戏登录业务,发起HTTP请求响应结果为空");
            return null;
        }
        logger.info("用户【" + username + "】调用IGPJ游戏登录业务,发起HTTP请求响应结果:{}", msg);
        JSONObject json = JSONObject.fromObject(msg);
        if (!"0".equals(json.getString("errorCode"))) {
            saveLog(msg, data, "loginGame");
        }
        return msg;
    }

    public String getBalance(String username, String password) {
        logger.info("用户【" + username + "】调用IGPJ游戏查询余额业务开始");
        password = DESEncrypt.getMd5(password);
        // 登录创建账号
        String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"GET_BALANCE\",\"params\":";
        data += "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}";
        logger.info("用户【" + username + "】调用IGPJ游戏查询余额,请求地址:{},请求参数:{}", apiurl, data);
        String msg = sendPost(apiurl, data);
        if (StringUtils.isBlank(msg)) {
            logger.error("用户【" + username + "】调用IG游戏查询余额,发起HTTP请求响应结果为空");
            return "error";
        }
        logger.info("用户【" + username + "】调用IG游戏查询余额,发起HTTP请求响应结果:{}", msg);
        JSONObject json = JSONObject.fromObject(msg);
        if (!"0".equals(json.getString("errorCode"))) {
            saveLog(msg, data, "getBalance");
        }
        return msg;
    }

    public String deposit(String username, String password, String billno, String amount) {
        logger.info("用户【" + username + "】调用IGPJ游戏上分业务开始,订单号:{}", billno);
        try {
            password = DESEncrypt.getMd5(password);
            // 登录创建账号
            String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"DEPOSIT\",\"params\":{\"username\":\""
                    + username + "\",\"password\":\"" + password + "\",";
            data += "\"ref\":\"" + billno + "\",\"desc\":\"\",\"amount\":\"" + amount + "\"}}";
            logger.info("IGPJ转账转出,post请求url" + apiurl + "参数" + data);
            String msg = sendPost(apiurl, data);
            if (ERROR.equals(msg)) {
                return ERROR;
            }
            if (StringUtils.isBlank(msg)) {
                logger.error("用户【" + username + "】调用IGPJ游戏上分,订单号【" + billno + "】发起HTTP请求响应结果为空");
                return "process";
            }
            JSONObject json = JSONObject.fromObject(msg);

            logger.info("用户【" + username + "】调用IGPJ游戏上分,订单号【" + billno + "】发起HTTP请求响应结果:{}", json);

            if ("0".equals(json.getString("errorCode"))) {
                return "success";
            }
            return "process";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "process";
        }

    }

    public String withdraw(String username, String password, String billno, String amount) {
        logger.info("用户【" + username + "】调用IGPJ游戏下分业务开始,订单号:{}", billno);
        try {
            password = DESEncrypt.getMd5(password);
            // 登录创建账号
            String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"WITHDRAW\",\"params\":{\"username\":\""
                    + username + "\",\"password\":\"" + password + "\",";
            data += "\"ref\":\"" + billno + "\",\"desc\":\"\",\"amount\":\"" + amount + "\"}}";
            logger.info("IGPJ转账游戏下分,post请求url" + apiurl + "参数" + data);
            String msg = sendPost(apiurl, data);
            if (ERROR.equals(msg)) {
                return ERROR;
            }
            JSONObject json = JSONObject.fromObject(msg);
            if (StringUtils.isBlank(msg)) {
                logger.error("用户【" + username + "】调用IGPJ游戏下分,订单号【" + billno + "】发起HTTP请求响应结果为空");
                return "process";
            }
            logger.info("用户【" + username + "】调用IGPJ游戏下分,订单号【" + billno + "】发起HTTP请求响应结果:{}", msg);
            if (!"0".equals(json.getString("errorCode"))) {
                saveLog(msg, data, "withdraw");
                return "process";
            }
            return "success";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "process";
        }
    }

    public String checkRef(String billno) {
        logger.info("查询IGPJ游戏转账订单,订单号:{}", billno);
        // 登录创建账号
        String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"CHECK_REF\",\"params\":{\"ref\":\"" + billno
                + "\"}}";
        logger.info("查询IGPJ游戏转账订单,post请求url" + apiurl + "参数" + data);
        try {
            String msg = sendPost(apiurl, data);
            if (StringUtils.isBlank(msg)) {
                logger.error("查询IGPJ游戏转账订单,订单号【" + billno + "】发起HTTP请求响应结果为空");
                return "process";
            }
            logger.info("查询IGPJ游戏转账订单,订单号【" + billno + "】发起HTTP请求响应结果:{}", msg);
            JSONObject json = JSONObject.fromObject(msg);
            String errorcode = json.getString("errorCode");
            if ("0".equals(errorcode) || "6601".equals(errorcode) || "6617".equals(errorcode)) {
                saveLog(msg, data, "checkRef");
            }
            return errorcode;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "process";
        }
    }

    /**
     * 发送请求到server端
     *
     * @param tagUrl 请求数据地址
     * @param data   请求数据
     * @return null发送失败，否则返回响应内容
     */
    public static String sendPost(String tagUrl, String data) {
        logger.info("IGPJ彩票请求URL:" + tagUrl + "  Data:" + data);
        HttpClient client = new HttpClient();
        // 创建post请求方法
        PostMethod myPost = new PostMethod(tagUrl);
        String responseString = null;
        int statusCode;
        try {
            // 设置请求头部类型
            myPost.setRequestHeader("Content-Type", "application/json");
            myPost.setRequestHeader("charset", "utf-8");
            myPost.setRequestBody(data);
            // 这里的超时单位是毫秒。这里的http.socket.timeout相当于SO_TIMEOUT
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            client.getHttpConnectionManager().getParams().setSoTimeout(10000);
            // 设置请求体，即xml文本内容，一种是直接获取xml内容字符串，一种是读取xml文件以流的形式
            statusCode = client.executeMethod(myPost);
            if (statusCode == HttpStatus.SC_OK) {
                InputStream inputStream = myPost.getResponseBodyAsStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                StringBuffer stringBuffer = new StringBuffer();
                String str;
                while ((str = br.readLine()) != null) {
                    stringBuffer.append(str);
                }
                responseString = stringBuffer.toString();
                logger.info("[IGPJ] HTTP请求服务器响应报文:{}" + responseString);
            } else {
                logger.info("[IGPJ] HTTP请求响应状态码异常：{}", statusCode);
                if ("2".equals(String.valueOf(statusCode).substring(0, 1))) {
                    return null;
                }
                responseString = "error";
            }
        } catch (Exception e) {
            logger.error("[IGPJ] HTTP请求失败：", e);
        } finally {
            myPost.releaseConnection();
            client.getHttpConnectionManager().closeIdleConnections(0);
        }
        return responseString;
    }

    private void saveLog(String msg, String data, String function) {
        FileLog f = new FileLog();
        Map<String, String> map = new HashMap<>();
        map.put("apiurl", apiurl);
        map.put("data", data);
        map.put("msg", msg);
        map.put("Function", function);
        f.setLog("IGPJ", map);
    }

}
