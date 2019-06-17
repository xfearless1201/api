package com.cn.tianxia.api.game.proxy;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.VRGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.service.UserService;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:34
 * @Description: VR游戏接口代理类
 */
@Service("VR")
public class VRGameServiceProxy implements GameInterfaceService {

    @Autowired
    private UserService userService;

    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        VRGameServiceImpl v = new VRGameServiceImpl(gameTransferVO.getConfig());
        String msg = v.Deposit(gameTransferVO.getBillno(), gameTransferVO.getAg_username(), Float.valueOf(gameTransferVO.getMoney()));
        if ("success".equalsIgnoreCase(msg)) {
            // 转账订单提交成功
            return TransferResponse.transferSuccess();
        } else {
            return TransferResponse.transferFaild();
        }
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        VRGameServiceImpl v = new VRGameServiceImpl(gameTransferVO.getConfig());
        String msg = v.Withdraw(gameTransferVO.getBillno(), gameTransferVO.getAg_username(), Float.valueOf(gameTransferVO.getMoney()));
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
        data.put("type", "VR");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            VRGameServiceImpl v = new VRGameServiceImpl(pmap);
//            String balance = v.getBalance(ag_username);
//            if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                return data;
//            }
            
            String balance = v.getBalance(ag_username);
            try {
                BigDecimal big = new BigDecimal(balance);
                if (big.compareTo(BigDecimal.ZERO) < 0) {
                    balance = "0.0";
                }
            } catch (Exception e) {
                balance = "0.0";
            }
            data.put("balance", balance);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("balance","0.00");
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
