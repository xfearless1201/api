package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.JDBGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:23
 * @Description: JDB游戏接口代理类
 */
@Service("JDB")
public class JDBGameServiceProxy implements GameInterfaceService {
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        JDBGameServiceImpl jdb = new JDBGameServiceImpl(gameTransferVO.getConfig());
        String msg = jdb.transIn(gameTransferVO.getAg_username(), gameTransferVO.getBillno(), Integer.valueOf(gameTransferVO.getMoney()));
        if ("success".equalsIgnoreCase(msg)) {
            // 成功
            return TransferResponse.transferSuccess();
        }

        if ("error".equalsIgnoreCase(msg)) {
            // 异常订单
            return TransferResponse.transferProcess();
        }
        return TransferResponse.transferFaild();
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        JDBGameServiceImpl jdb = new JDBGameServiceImpl(gameTransferVO.getConfig());
        String msg = jdb.transOut(gameTransferVO.getAg_username(), gameTransferVO.getBillno(), Integer.valueOf(gameTransferVO.getMoney()));
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

    /**
     * 获取余额接口
     */
    @Override
    public JSONObject getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        JSONObject data = new JSONObject();
        data.put("type", "JDB");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            //获取游戏余额
            JDBGameServiceImpl jdb = new JDBGameServiceImpl(pmap);
//            String balance = jdb.getBalance(ag_username);
//            if(!balance.equalsIgnoreCase("error") && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                return data;
//            }
            
            String balance = jdb.getBalance(ag_username);
            if ("error".equals(balance)) {
                data.put("balance", "维护中");
                return data;
            }
            data.put("balance", balance);
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
