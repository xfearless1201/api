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
 * @ClassName IGServiceImpl
 * @Description IG彩票
 * @Date 2019年2月9日 下午4:32:12
 */
public class IGGameServiceImpl {
    /*
     * private static final String
     * apiurl="http://igyfsw.iasia99.com/igapiyf/app/api.do"; private static
     * final String hashcode="ttx_2e823371-a192-40db-b8ad-9904edd8";
     */
    private static String apiurl;// 命令(3) (4) (5) (6)对接地址为
    private static String hashcode;
    private static int line;
    private static String lotto_url; // 香港彩 命令(1) (2) 对接地址为
    private static String lottery_url; // 时时彩 命令(1) (2) 对接地址为
    private static String gfc_url; // 官方彩
    private static Logger logger = LoggerFactory.getLogger(IGGameServiceImpl.class);
    /**
     * (1)LOGIN命令 (2)CHANGE_PASSWORD命令 (3)GET_BALANCE 命令 (4)deposit 命令
     * (5)withdraw 命令 (6)CHECK_REF命令
     */
    DESEncrypt d = new DESEncrypt("");

    public IGGameServiceImpl(Map<String, String> pmap, String cagent) {
        PlatFromConfig pf = new PlatFromConfig();
        pf.InitData(pmap, "IG");
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
        gfc_url = jo.getString("gfc_url");
        try {
            line = jo.getInt("line");
        } catch (Exception e) {
            line = 1;
        }
    }


    public String loginGame(String username, String password, String gameType, String gameId, String type, String handicap) {
        logger.info("用户【" + username + "】调用IG游戏登录业务开始");
        password = DESEncrypt.getMd5(password);
        
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", username);
        jsonObject.put("password", password);
        jsonObject.put("currency", "CNY");
        jsonObject.put("language", "CN");
        jsonObject.put("gameType", gameType);
        jsonObject.put("line", line);
        jsonObject.put("userCode", username);
        jsonObject.put("nickname", username);

        if ("LOTTERY".equals(gameType)) {
            jsonObject.put("lotteryTray", handicap);
            jsonObject.put("lotteryPage", gameId);
            jsonObject.put("lotteryType", type);
            jsonObject.put("lobby", "1");
        }
        if ("LOTTO".equals(gameType)) {
            //香港彩不分电脑端手机端
            jsonObject.put("lottoTray", handicap);
        }
        if ("GFC".equals(gameType)){
            jsonObject.put("gfcTray",handicap);
            jsonObject.put("gfcType",type);
            jsonObject.put("gfcPage",gameId);
        }
        String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"LOGIN\",\"params\":" + jsonObject + "}";
        logger.info("用户【" + username + "】调用IG游戏登录业务,请求参数:{}", data);
        // 香港彩票
        if ("LOTTO".equals(gameType)) {
            apiurl = lotto_url;
            // 时时彩
        } else if ("LOTTERY".equals(gameType)) {
            apiurl = lottery_url;
        } else if ("GFC".equals(gameType)){
            //官方彩
            apiurl = gfc_url;
        }
        logger.info("用户【" + username + "】调用IG游戏登录业务,请求地址:{}", apiurl);
        String msg = sendPost(apiurl, data);
        if (StringUtils.isBlank(msg)) {
            logger.error("用户【" + username + "】调用IG游戏登录业务,发起HTTP请求响应结果为空");
            return null;
        }
        JSONObject json = JSONObject.fromObject(msg);
        logger.info("用户【" + username + "】调用IG游戏登录业务,发起HTTP请求响应结果:{}", json);

        if (!"0".equals(json.getString("errorCode"))) {
            FileLog f = new FileLog();
            Map<String, String> map = new HashMap<>();
            map.put("apiurl", apiurl);
            map.put("data", data);
            map.put("msg", msg);
            map.put("Function", "loginGame");
            f.setLog("IG", map);
        }
        return msg;
    }

    public String getBalance(String username, String password) {
        logger.info("用户【" + username + "】调用IG游戏查询余额业务开始");
        password = DESEncrypt.getMd5(password);
        // 登录创建账号
        String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"GET_BALANCE\",\"params\":";
        data += "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}";
        logger.info("用户【" + username + "】调用IG游戏查询余额,请求地址:{},请求参数:{}", apiurl, data);
        String msg = sendPost(apiurl, data);
        JSONObject json = JSONObject.fromObject(msg);
        if (StringUtils.isBlank(msg)) {
            logger.error("用户【" + username + "】调用IG游戏查询余额,发起HTTP请求响应结果为空");
            return "error";
        }
        logger.info("用户【" + username + "】调用IG游戏查询余额,发起HTTP请求响应结果:{}", json);
        if (!"0".equals(json.getString("errorCode"))) {
            FileLog f = new FileLog();
            Map<String, String> map = new HashMap<>();
            map.put("apiurl", apiurl);
            map.put("data", data);
            map.put("msg", msg);
            map.put("Function", "getBalance");
            f.setLog("IG", map);
        }
        return msg;
    }

    public String deposit(String username, String password, String billno, String amount) {
        logger.info("用户【" + username + "】调用IG游戏上分业务开始,订单号:{}", billno);
        try {
            password = DESEncrypt.getMd5(password);
            // 登录创建账号
            String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"DEPOSIT\",\"params\":{\"username\":\""
                    + username + "\",\"password\":\"" + password + "\",";
            data += "\"ref\":\"" + billno + "\",\"desc\":\"\",\"amount\":\"" + amount + "\"}}";
            logger.info("IG转账转出,post请求url" + apiurl + "参数" + data);
            String msg = sendPost(apiurl, data);
            if ("error".equals(msg)) {
                return "faild";
            }
            if (StringUtils.isBlank(msg)) {
                logger.error("用户【" + username + "】调用IG游戏上分,订单号【" + billno + "】发起HTTP请求响应结果为空");
                return "process";
            }
            logger.info("用户【" + username + "】调用IG游戏上分,订单号【" + billno + "】发起HTTP请求响应结果:{}", JSONObject.fromObject(msg));
            //解析响应结果
            JSONObject json = JSONObject.fromObject(msg);
            logger.info("[IG] HTTP响应上分报文：{}", json);
            if ("0".equals(json.getString("errorCode"))) {
                return "success";
            }
            logger.info("[IG] 转账响应状态为失败！返回process再次确认订单状态！ IG响应状态：{}", json.getString("errorCode"));
        } catch (Exception e) {
            logger.info("[IG] 业务系统错误！", e);
        }

        return "process";
    }

    public String withdraw(String username, String password, String billno, String amount) {
        logger.info("用户【" + username + "】调用IG游戏下分业务开始,订单号:{}", billno);
        try {
            password = DESEncrypt.getMd5(password);
            // 登录创建账号
            String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"WITHDRAW\",\"params\":{\"username\":\""
                    + username + "\",\"password\":\"" + password + "\",";
            data += "\"ref\":\"" + billno + "\",\"desc\":\"\",\"amount\":\"" + amount + "\"}}";
            logger.info("IG转账游戏下分,post请求url" + apiurl + "参数" + data);
            String msg = sendPost(apiurl, data);
            if ("error".equals(msg)) {
                return "faild";
            }
            if (StringUtils.isBlank(msg)) {
                logger.error("用户【" + username + "】调用IG游戏下分,订单号【" + billno + "】发起HTTP请求响应结果为空");
                return "process";
            }
            logger.info("用户【" + username + "】调用IG游戏下分,订单号【" + billno + "】发起HTTP请求响应结果:{}", msg);
            //解析响应结果
            JSONObject json = JSONObject.fromObject(msg);
            logger.info("[IG] HTTP响应下分报文：{}", json);
            if ("0".equals(json.getString("errorCode"))) {
                return "success";
            }
            logger.info("[IG] 转账响应状态为失败！返回process再次确认订单状态！ IG响应状态：{}", json.getString("errorCode"));
        } catch (Exception e) {
            logger.info("[IG] 游戏下分错误：", e.getMessage());
            e.printStackTrace();
        }
        return "process";
    }

    public String checkRef(String billno) {
        logger.info("查询IG游戏转账订单,订单号:{}", billno);
        // 登录创建账号
        String data = "{\"hashCode\":\"" + hashcode + "\",\"command\":\"CHECK_REF\",\"params\":{\"ref\":\"" + billno
                + "\"}}";
        logger.info("查询IG游戏转账订单,post请求url" + apiurl + "参数" + data);
        try {
            String msg = sendPost(apiurl, data);
            if (StringUtils.isBlank(msg)) {
                logger.error("查询IG游戏转账订单,订单号【" + billno + "】发起HTTP请求响应结果为空");
                return "process";
            }
            logger.info("查询IG游戏转账订单,订单号【" + billno + "】发起HTTP请求响应结果:{}", msg);
            JSONObject json = JSONObject.fromObject(msg);
            String errorcode = json.getString("errorCode");
            if ("0".equals(errorcode) || "6614".equals(errorcode)) {
                return "faild";
            }
            if ("6601".equals(errorcode)) {
                return "success";
            }
            if ("6617".equals(errorcode)) {
                return "process";
            }

            logger.info("[IG]无法识别订单【" + billno + "】 响应的状态码:{}", errorcode);
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }

        return "process";
    }

    /**
     * 发送请求到server端
     *
     * @param
     * @param
     * @return null发送失败，否则返回响应内容
     */
    public static String sendPost(String tagUrl, String data) {
        // 创建httpclient工具对象
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
            //这里的超时单位是毫秒。这里的http.socket.timeout相当于SO_TIMEOUT  读取时间设置5秒
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
                logger.info("[IG] HTTP请求响应结果：{}", responseString);
            } else {
                logger.info("[IG] 发起HTTP请求无效,响应状态值：{}", statusCode);
                if (String.valueOf(statusCode).startsWith("2")) {
                    return null;
                }
                return "error";
            }
        } catch (Exception e) {
            logger.error("[IG] 发起HTTP请求错误！", e.getMessage());
            e.printStackTrace();
        } finally {
            myPost.releaseConnection();
            client.getHttpConnectionManager().closeIdleConnections(0);
        }
        return responseString;
    }

}
