package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.LYQPGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:38
 * @Description: LYQP游戏接口代理类
 */
@Service("LYQP")
public class LYQPGameServiceProxy implements GameInterfaceService {
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        LYQPGameServiceImpl lyqp = new LYQPGameServiceImpl(gameTransferVO.getConfig());
        String msg = lyqp.channelHandleOn(gameTransferVO.getAg_username(), gameTransferVO.getBillno(), gameTransferVO.getMoney(), "2");
        if ("success".equals(msg)) {
            // 转账订单提交成功
            return TransferResponse.transferSuccess();
        } else if ("fail".equals(msg)) {
            return TransferResponse.transferFaild();
        } else {
            return TransferResponse.transferProcess();
        }
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        LYQPGameServiceImpl lyqp = new LYQPGameServiceImpl(gameTransferVO.getConfig());
        String msg = lyqp.channelHandleOn(gameTransferVO.getAg_username(), gameTransferVO.getBillno(), gameTransferVO.getMoney(), "3");
        if ("success".equals(msg)) {
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
        data.put("type", "LYQP");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            LYQPGameServiceImpl lyqp = new LYQPGameServiceImpl(pmap);
//            String balance = lyqp.queryUnderTheBalance(ag_username);
//            if(!balance.equalsIgnoreCase("error")){
//                balance = JSONObject.fromObject(balance).getJSONObject("d").getString("money");
//                if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                    data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                    return data;
//                }
//            }
            
            String balance = lyqp.queryUnderTheBalance(ag_username);
            if ("error".equals(balance)) {
                data.put("balance", "维护中");
            } else {
                balance = JSONObject.fromObject(balance).getJSONObject("d").getString("money");
                data.put("balance", balance);
            }
            return data;
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
