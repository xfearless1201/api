package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.SBService;
import com.cn.tianxia.api.game.impl.SBGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:17
 * @Description: SB游戏接口代理类
 */
@Service("SB")
public class SBGameServiceProxy implements GameInterfaceService {
    
    private static final Logger logger = LoggerFactory.getLogger(SBGameServiceProxy.class);
    
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        SBService s = new SBGameServiceImpl(gameTransferVO.getConfig());
        // 获取平台授权token
        String atoken = s.getAccToken();
        // 获取token
        String access_token = JSONObject.fromObject(atoken).getString("access_token");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");
        Date now = new Date();
        Calendar Cal = Calendar.getInstance();
        Cal.setTime(now);
        Cal.add(Calendar.HOUR_OF_DAY, -7);
        String todayStr = sdf.format(Cal.getTime());
        String msg = s.WalletCredit(gameTransferVO.getAg_username(), gameTransferVO.getBillno(), gameTransferVO.getMoney() + "", todayStr, access_token);
        if (StringUtils.isBlank(msg)) {
            return TransferResponse.transferProcess();
        } else if ("false".equalsIgnoreCase(JSONObject.fromObject(msg).getString("dup"))) {
            // 转账成功
            return TransferResponse.transferSuccess();
        } else {
            return TransferResponse.transferFaild();
        }
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        SBService s = new SBGameServiceImpl(gameTransferVO.getConfig());
        // 获取平台授权token
        String atoken = s.getAccToken();
        JSONObject json = new JSONObject();
        json = JSONObject.fromObject(atoken);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");
        Date now = new Date();
        Calendar Cal = Calendar.getInstance();
        Cal.setTime(now);
        Cal.add(Calendar.HOUR_OF_DAY, -7);
        String todayStr = sdf.format(Cal.getTime());
        String msg = s.WalletDebit(gameTransferVO.getAg_username(), gameTransferVO.getBillno(), gameTransferVO.getMoney() + "", todayStr, json.get("access_token").toString());
        json = JSONObject.fromObject(msg);
        // false代表提交成功
        if ("false".equals(json.getString("dup"))) {
            return TransferResponse.transferSuccess();
        } else {
            return TransferResponse.transferFaild();
        }
    }

    @Override
    public JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        JSONObject data = new JSONObject();
        data.put("type", "SB");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            SBService s = new SBGameServiceImpl(pmap);
            String atoken = s.getAccToken();
            logger.info("[SB]申博游戏获取用户余额获取token响应结果:{}",atoken);
//            if(StringUtils.isNotBlank(atoken)){
//                JSONObject json = JSONObject.fromObject(atoken);
//                atoken = json.get("access_token").toString();
//                String j = s.getBalance(ag_username, atoken);
//                logger.info("[SB]申博游戏获取用户余额响应结果:{}",j);
//                json = JSONObject.fromObject(j);
//                String balance = json.getString("bal");
//                if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                    data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                    return data;
//                }
//            }
            JSONObject json = new JSONObject();
            json = JSONObject.fromObject(atoken);
            atoken = json.get("access_token").toString();
            String j = s.getBalance(ag_username, atoken);
            json = JSONObject.fromObject(j);
            if (json.get("bal").toString() == null || json.get("bal").toString() == "") {

                data.put("balance", "0.00");
                return data;
            }
            data.put("balance", json.get("bal").toString());
        } catch (Exception e) {
            e.printStackTrace();
            data.put("balance", "维护中");
        }

        return data;
    }

    @Override
    public JSONObject checkOrCreateAccount(GameCheckOrCreateVO gameCheckOrCreateVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject queryTransferOrder(GameQueryOrderVO gameQueryOrderVO) throws Exception {
        return null;
    }
}
