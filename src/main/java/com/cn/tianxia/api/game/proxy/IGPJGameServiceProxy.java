package com.cn.tianxia.api.game.proxy;

import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.IGPJGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.*;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:30
 * @Description: IGPJ游戏接口代理类
 */
@Service("IGPJ")
public class IGPJGameServiceProxy implements GameInterfaceService {
    private static Logger logger = LoggerFactory.getLogger(IGPJGameServiceProxy.class);
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        IGPJGameServiceImpl c = new IGPJGameServiceImpl(gameTransferVO.getConfig(),gameTransferVO.getAg_username().substring(0,3));
        String msg = c.deposit(gameTransferVO.getAg_username(), gameTransferVO.getPassword(), gameTransferVO.getBillno(), gameTransferVO.getMoney());
        if ("success".equalsIgnoreCase(msg)) {
            return TransferResponse.transferSuccess();
        }
        if(msg.equals("error"))   return TransferResponse.transferFaild();
        // 轮询
        boolean isPoll = true;
        int polls = 0;
        do {
            Thread.sleep(3000);
            logger.info("用户:【"+gameTransferVO.getAg_username()+"】,订单号:【"+gameTransferVO.getBillno()+"】,操作天下平台向游戏平台:【"+gameTransferVO.getType()+"】转入金额业务(游戏上分)业务,第{}次轮询转账订单次数", polls);
            polls++;
            String ckeckMsg = c.checkRef(gameTransferVO.getBillno());
            if ("6601".endsWith(ckeckMsg)) {
                // 转账成功
                return TransferResponse.transferSuccess();
            } else if ("6617".endsWith(ckeckMsg)) {
                if (polls > 2) {
                    return TransferResponse.transferProcess();
                }
            } else if ("0".equals(ckeckMsg)){
                return TransferResponse.transferFaild();
            }
        } while (isPoll);

        return TransferResponse.transferProcess();
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        String msg = "";
        IGPJGameServiceImpl c = new IGPJGameServiceImpl(gameTransferVO.getConfig(),gameTransferVO.getAg_username().substring(0,3));
        msg = c.withdraw(gameTransferVO.getAg_username(), gameTransferVO.getPassword(), gameTransferVO.getBillno(), gameTransferVO.getMoney());
        if(msg.equals("error")) return  TransferResponse.transferFaild();
        if ("success".equals(msg)) {
            return TransferResponse.transferSuccess();
        } else {
            //轮询
            boolean isPoll = true;
            int polls = 0;
            do {
                Thread.sleep(2000);
                logger.info("用户:【"+gameTransferVO.getAg_username()+"】,订单号:【"+gameTransferVO.getBillno()+"】,操作天下平台向游戏平台:【"+gameTransferVO.getType()+"】转入金额业务(游戏上分)业务,第{}次轮询转账订单次数", polls);
                polls++;
                msg = c.checkRef(gameTransferVO.getBillno());
                //6601为该单据已成功,6607为处理中,2秒后再次查询该订单状态
                if ("6601".endsWith(msg)) {
                    return TransferResponse.transferSuccess();
                } else {
                    if(polls > 2){
                        return TransferResponse.transferFaild();
                    }
                }
            } while (isPoll);
        }
        return TransferResponse.transferFaild();
    }

    @Override
    public JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        JSONObject data = new JSONObject();
        data.put("type", "IGPJ");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String ag_password = gameBalanceVO.getPassword();
            IGPJGameServiceImpl c = new IGPJGameServiceImpl(pmap,ag_username.substring(0, 3));
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
