package com.cn.tianxia.api.game.gameProxy;

import com.alibaba.fastjson.JSONObject;
import com.cn.tianxia.api.common.v2.OkHttpClient;
import com.cn.tianxia.api.common.v2.OkHttpUtils;
import com.cn.tianxia.api.domain.txdata.v2.PlatformConfigDao;
import com.cn.tianxia.api.game.GameFactoryService;
import com.cn.tianxia.api.project.v2.PlatformConfigEntity;
import com.cn.tianxia.api.utils.RedisUtils;
import com.cn.tianxia.api.vo.v2.*;

import org.apache.axis.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @ClassName IGPJServiceImpl
 * @Description NWG 新世界棋牌
 * @author Jacky
 * @Date 2019年5月18日 下午4:31:46
 * @version 1.0.0
 */
@Service("NWGS")
public class NWGGameServiceImpl implements GameFactoryService {

    private final static Logger logger = LoggerFactory.getLogger(NWGGameServiceImpl.class);

    @Autowired
    private PlatformConfigDao platformConfigDao;

    @Autowired
    private OkHttpUtils okHttpUtils;

    @Autowired
    private RedisUtils redisUtils;

    static SimpleDateFormat simpleDateFormat;
    static StringBuilder     stringBuilder;

    static {
        simpleDateFormat  = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        stringBuilder     = new StringBuilder(50);
    }
    private static  JSONObject configJson;
    private static final String PARAM_AGENT = "agent=";
    private static final String PARAM_TIMESTAMP = "timestamp=";
    private static final String PARAM_PARAM = "param=";
    private static final String PARAM_KEY = "key=";
    private static final String  ERROR = "error";
    private static final String  SUCCESS = "success";
    private static final String  FAILD = "faild";
    private static final String  PROCESS = "process";

    private static final String  AGENT = "agent";
    private static final String  DESKEY = "deskey";
    private static final String  MD5KEY = "md5key";
    private static final String  API_URL = "api_url";



    private void init()throws  Exception{
        JSONObject jsonObject = redisUtils.get("NWG",JSONObject.class);
        if(jsonObject != null)  {
            configJson = jsonObject;
        }else {
            PlatformConfigEntity platformConfigEntity = platformConfigDao.selectByPlatformKey("NWG");
            if(platformConfigEntity == null)
                  throw new Exception("[NWG] 游戏配置信息为空！ 调用游戏业务实现失败！");

            redisUtils.set(platformConfigEntity.getPlatformKey(),platformConfigEntity.getPlatformConfig());
            configJson = JSONObject.parseObject(platformConfigEntity.getPlatformConfig());

        }

    }


    @Override
    public String transferIn(GameTransferVO gameTransferVO) throws Exception {
        init();
        return transfer(gameTransferVO,"2");
    }

    @Override
    public String transferOut(GameTransferVO gameTransferVO) throws Exception {
        init();
        return  transfer(gameTransferVO,"3");
    }

    public String transfer(GameTransferVO gameTransferVO ,String  s)throws  Exception{
        logger.info("[NWG] transferIn(GameTransferVO gameTransferVO =  {} -start",gameTransferVO);
        if(StringUtils.isEmpty(gameTransferVO.getAg_username())||
                StringUtils.isEmpty(gameTransferVO.getMoney() )||
                StringUtils.isEmpty(gameTransferVO.getBillno()))  throw  new Exception("[NWG] 游戏上下分失败,上下分所需参数不足！");

            String timestamp = String.valueOf(System.currentTimeMillis());

            stringBuilder = new StringBuilder().append("s=").append(s).append("&")
                    .append("account=").append(gameTransferVO.getAg_username()).append("&")
                    .append("money=").append(gameTransferVO.getMoney()).append("&")
                    .append("orderid=").append(gameTransferVO.getBillno());

            String url = formatUrl(timestamp,
                    AESEncrypt(stringBuilder.toString(),configJson.getString(DESKEY)),
                    MD5(configJson.getString(AGENT) +timestamp + configJson.getString(MD5KEY))).toString();
            logger.info("[NWG] HTTP请求URL：{}",url);
        try{
            String responseString = okHttpUtils.sendGet(url);

            if(responseString == null)
                return  loopOrder(gameTransferVO.getBillno());

            JSONObject json = JSONObject.parseObject(responseString);
            if(json.containsKey("d")){
                JSONObject data = JSONObject.parseObject(json.getString("d"));

                if(data.getString("code").equals("0")){
                    return  loopOrder(gameTransferVO.getBillno());
                }
            }

        }catch (Exception e){
            logger.error("[NWG] 游戏上下分错误：",e);
        }
        return  PROCESS;
    }

    private String loopOrder(String orderNo) throws Exception{
        logger.info("[NWG] loopOrder(String orderNo = {} -start",orderNo);
        int loop = 0;
        if(querOderStatus(orderNo) == SUCCESS) return SUCCESS;

        while (loop < 2){
            Thread.sleep(3000);
            loop++;
            String orderStatus = querOderStatus(orderNo);
            if(orderStatus == SUCCESS)  return  SUCCESS;
            if(orderStatus == FAILD)    return  FAILD;
        }
        return  PROCESS;
    }


    public String querOderStatus(String  orderNo)throws  Exception{
        logger.info("[NWG] querOderStatus(String  orderNo = {} -start",orderNo);

        String timestamp = String.valueOf(System.currentTimeMillis());
        stringBuilder = new StringBuilder("s=4").append("&").append("orderid=").append(orderNo);

        String url =  formatUrl(timestamp,AESEncrypt(stringBuilder.toString(),configJson.getString(DESKEY)),
                MD5(configJson.getString(AGENT) + timestamp + configJson.getString(MD5KEY))).toString();
        logger.info("[NWG] HTTP请求URL:{}",url);

        String responseString = okHttpUtils.sendGet(url);
        logger.info("[NWG] HTTP响应报文：{}",responseString);
        if(responseString == null )  return  PROCESS;

        JSONObject jsonObject = JSONObject.parseObject(responseString);
        if(jsonObject.containsKey("d")){

            JSONObject data = JSONObject.parseObject(jsonObject.getString("d"));
            if(data.getString("code").equals("0"))   return  SUCCESS;
            if(data.getString("code").equals("2"))  return  FAILD;
        }
        return  PROCESS;
    }


    @Override
    public JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception {
        return null;
    }

    @Override
    public String getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        logger.info("getBalance(GameBalanceVO gameBalanceVO = {}",gameBalanceVO);
        if(StringUtils.isEmpty(gameBalanceVO.getGamename()))  throw  new Exception("[NWG] 玩家账号不能为空!");
        init();
        String timestamp = String.valueOf(System.currentTimeMillis());
        stringBuilder = new StringBuilder().append("s=1").append("&").append("account=").append(gameBalanceVO.getGamename());
        String url = formatUrl(timestamp,AESEncrypt(stringBuilder.toString(),configJson.getString(DESKEY)),
                MD5(configJson.getString(AGENT)+ timestamp + configJson.getString(MD5KEY))).toString();

        String balance = ERROR;
        try{
            String  responseJson = okHttpUtils.sendGet(url);
            logger.info("[NWG] 用["+gameBalanceVO.getGamename()+"]  HTTP响应报文：{}",responseJson);

            JSONObject jsonObject = JSONObject.parseObject(responseJson);
            if(jsonObject.containsKey("d")){

                JSONObject data = JSONObject.parseObject(jsonObject.getString("d"));
                if(data.getString("code").equals("0")){
                    balance = data.getString("money");
                }
            }
        }catch (Exception e){
            logger.error("[NWG] 获取游戏余额失败！",e.getMessage());
        }

        return  balance;
    }

    @Override
    public String checkOrCreateAccount(GameForwardVO gameForwardVO) throws Exception {
        logger.info("checkOrCreateAccount(GameForwardVO gameBalanceVO = {}",gameForwardVO);
        if (StringUtils.isEmpty(gameForwardVO.getAg_username())||StringUtils.isEmpty(gameForwardVO.getGameId()))
            throw  new Exception("[NWG] 游戏账号或游戏ID不能为空！");
        String timestamp = String.valueOf(System.currentTimeMillis());
        init();
        stringBuilder = new StringBuilder()
                .append("s=0").append("&")
                .append("account=").append(gameForwardVO.getAg_username()).append("&")
                .append("money=0").append("&")
                .append("orderid=").append(AGENT+ simpleDateFormat.format(new Date())+ gameForwardVO.getAg_username()).append("&")
                .append("ip=").append(gameForwardVO.getIp()).append("&")
                .append("lineCode=1").append("&")
                .append("KindID=").append(gameForwardVO.getGameId()) ;

        logger.info("[NWG] 代加密参数串:{}",stringBuilder.toString());
        String url = formatUrl(timestamp,AESEncrypt(stringBuilder.toString(),configJson.getString(DESKEY)),
                MD5(configJson.getString(AGENT) + timestamp + configJson.getString(MD5KEY))).toString();
        String gameUrl = ERROR;
        try{
            String  responseJson = okHttpUtils.sendGet(url);
            logger.info("[NWG] 用户["+gameForwardVO.getAg_username()+"] HTTP响应报文：{}",responseJson);

            if(responseJson != null) {
                JSONObject jsonObject = JSONObject.parseObject(responseJson);
                if(jsonObject.containsKey("d")){
                    JSONObject jsonData =  JSONObject.parseObject(jsonObject.get("d").toString());
                    if(jsonData.containsKey("code")&& jsonData.get("code").toString().equals("0")){
                        gameUrl = jsonData.getString("url");
                    }
                }
            }
        }catch (Exception e){
            logger.error("[NWG] 获取游戏URL失败！",e.getMessage());
        }
        return gameUrl;
    }

    @Override
    public JSONObject queryTransferOrder(GameQueryOrderVO gameQueryOrderVO) throws Exception {
        return null;
    }



    StringBuilder formatUrl(String timestamp,String param,String key){

        return  stringBuilder = new StringBuilder(configJson.getString(API_URL)).append(PARAM_AGENT).append(configJson.getString(AGENT)).append("&")
                .append(PARAM_TIMESTAMP).append(timestamp).append("&")
                .append(PARAM_PARAM).append(param).append("&")
                .append(PARAM_KEY).append(key);
    }

    private static String MD5(String sourceStr) throws  Exception {
        logger.info("NWGMD5(String sourceStr = {} -start", sourceStr);
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes("UTF-8"));
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0 , length  = b.length; offset < length ; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("[NWG] 加密错误"+ e.getMessage(),e);
            throw  e;
        } catch (UnsupportedEncodingException e) {
            logger.error("[MWG] 转换字节码错误！"+ e.getMessage(),e);
            throw  e;
        }
        return result;
    }


    private static String AESEncrypt(String value,String key) throws Exception {
        logger.info("[NWG] AESEncrypt(String value,String key = {},{} -start" , value, key);

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        byte[] raw = key.getBytes("UTF-8");

        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

        byte[] encrypted = cipher.doFinal(value.getBytes("UTF-8"));

        String base64 = new BASE64Encoder().encode(encrypted);

        return URLEncoder.encode(base64, "UTF-8");

    }
}
