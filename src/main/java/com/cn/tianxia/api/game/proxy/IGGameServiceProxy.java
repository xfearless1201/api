package com.cn.tianxia.api.game.proxy;

import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.IGGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.*;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:28
 * @Description: IG游戏接口代理类
 */
@Service("IG")
public class IGGameServiceProxy implements GameInterfaceService {

    private static final Logger logger = LoggerFactory.getLogger(IGGameServiceProxy.class);

    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        Map<String,String> data = gameTransferVO.getConfig();
        String username = gameTransferVO.getUsername();
        String ag_username = gameTransferVO.getAg_username();
        String billno = gameTransferVO.getBillno();
        String credit = gameTransferVO.getMoney();
        String ag_password = gameTransferVO.getPassword();
        String type = gameTransferVO.getType();
        IGGameServiceImpl c = new IGGameServiceImpl(data,gameTransferVO.getAg_username().substring(0,3));
        String msg = c.deposit(ag_username, ag_password, billno, credit + "");
        if ("success".equalsIgnoreCase(msg)) {
            return TransferResponse.transferSuccess();
        }

        if(msg.equals("faild")) return TransferResponse.transferFaild();

        int polls = 0;
        for (;;){
            Thread.sleep(3000);
            logger.info("用户:【"+username+"】,订单号:【"+billno+"】,操作天下平台向游戏平台:【"+type+"】转入金额业务(游戏上分)业务,第{}次轮询转账订单次数", polls);
            polls++;
            String ckeckMsg = c.checkRef(billno);
            if (ckeckMsg.equals("success"))  return TransferResponse.transferSuccess();
            if (ckeckMsg.equals("faild"))    return TransferResponse.transferFaild();
            if (ckeckMsg.equals("process") && polls>2)   return TransferResponse.transferProcess();
        }
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        Map<String, String> data = gameTransferVO.getConfig();
        String username = gameTransferVO.getUsername();
        String ag_username = gameTransferVO.getAg_username();
        String billno = gameTransferVO.getBillno();
        String credit = gameTransferVO.getMoney();
        String ag_password = gameTransferVO.getPassword();
        String type = gameTransferVO.getType();
        IGGameServiceImpl c = new IGGameServiceImpl(data,gameTransferVO.getAg_username().substring(0,3));
        String msg = c.withdraw(ag_username, ag_password, billno, credit + "");
        if ("success".equals(msg)) return TransferResponse.transferSuccess();
        if(msg.equals("faild"))    return TransferResponse.transferFaild();

        int polls = 0;
        for (;;){
            Thread.sleep(2500);
            logger.info("用户:【"+username+"】,订单号:【"+billno+"】,操作天下平台向游戏平台:【"+type+"】转入金额业务(游戏上分)业务,第{}次轮询转账订单次数", polls);
            polls++;
            String ckeckMsg = c.checkRef(billno);
            if (ckeckMsg.equals("success"))  return TransferResponse.transferSuccess();
            if (ckeckMsg.equals("faild"))    return TransferResponse.transferFaild();
            if (polls>2)   return TransferResponse.transferProcess();
        }
    }

    @Override
    public JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        JSONObject data = new JSONObject();
        data.put("type", "IG");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String ag_password = gameBalanceVO.getPassword();
            IGGameServiceImpl c = new IGGameServiceImpl(pmap,ag_username.substring(0, 3));
//            String msg = c.getBalance(ag_username, ag_password);
//            if(StringUtils.isNotEmpty(msg)){
//                //解析想结果
//                JSONObject jsonObject = JSONObject.fromObject(msg);
//                if(jsonObject.getInt("errorCode") == 0){
//                    JSONObject balanceJson = jsonObject.getJSONObject("params");
//                    if(balanceJson.containsKey("balance")){
//                        String balance = balanceJson.getString("balance");
//                        if(StringUtils.isNotBlank(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                            data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                            return data;
//                        }
//                    }
//                }
//            }
            String msg = c.getBalance(ag_username, ag_password);
            JSONObject json;
            json = JSONObject.fromObject(msg);
            if ("0".equals(json.get("errorCode").toString())) {
                json = json.getJSONObject("params");
                data.put("balance", json.get("balance").toString());
                return data;
            } else {
                data.put("balance", "维护中");
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("balance", "0.00");
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
