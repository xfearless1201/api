package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.IMONEGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * IM 游戏入口
 * jacky
 */
@Service("IM")
public class IMGameServiceProxy implements GameInterfaceService {
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        IMONEGameServiceImpl imoneGameService = new IMONEGameServiceImpl(gameTransferVO.getConfig());
        String status = imoneGameService.transferIn(gameTransferVO);
        if(status == "success")  return TransferResponse.transferSuccess();
        if(status == "faild")    return TransferResponse.transferFaild();
        return  TransferResponse.transferProcess();
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        IMONEGameServiceImpl imoneGameService = new IMONEGameServiceImpl(gameTransferVO.getConfig());
        String status = imoneGameService.transferOut(gameTransferVO);
        if(status == "success")  return TransferResponse.transferSuccess();
        if(status == "faild")    return TransferResponse.transferFaild();
        return  TransferResponse.transferProcess();
    }

    @Override
    public JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        JSONObject data = new JSONObject();
        data.put("type", "IM");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String ag_password = gameBalanceVO.getPassword();
            IMONEGameServiceImpl imoneGameService = new IMONEGameServiceImpl(pmap);
//            String msg = imoneGameService.getBalance(ag_username);
//            if(StringUtils.isNotEmpty(msg) && PatternUtils.isMatch(msg, PatternUtils.MONEYREGEX)){
//                data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(msg)));
//                return data;
//            }
            
            data.put("balance",imoneGameService.getBalance(ag_username) != null?imoneGameService.getBalance(ag_username):"维护中");
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
