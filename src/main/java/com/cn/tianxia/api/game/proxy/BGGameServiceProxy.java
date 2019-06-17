package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.BGGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:33
 * @Description: BG游戏接口代理类
 */
@Service("BG")
public class BGGameServiceProxy implements GameInterfaceService {
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        Map<String,String> data = gameTransferVO.getConfig();
        String ag_username = gameTransferVO.getAg_username();
        String billno = gameTransferVO.getBillno();
        String credit = gameTransferVO.getMoney();
        BGGameServiceImpl c = new BGGameServiceImpl(data);
        String msg = c.openBalanceTransfer(ag_username, "open.balance.transfer", billno, credit + "", "IN",
                billno);
        if ("success".equalsIgnoreCase(msg)) {
            // 转账订单提交成功
            return TransferResponse.transferSuccess();
        } else {
            return TransferResponse.transferFaild();
        }
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        Map<String,String> data = gameTransferVO.getConfig();
        String ag_username = gameTransferVO.getAg_username();
        String billno = gameTransferVO.getBillno();
        String credit = gameTransferVO.getMoney();
        String msg = "";
        BGGameServiceImpl c = new BGGameServiceImpl(data);
        msg = c.openBalanceTransfer(ag_username, "open.balance.transfer", billno, credit + "", "OUT", billno);
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
        data.put("type", "BG");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String tempContextUrl = gameBalanceVO.getTempContextUrl();
            BGGameServiceImpl c = new BGGameServiceImpl(pmap);
//            JSONObject jsonObjec = c.openUserCommonAPI(ag_username, "open.balance.get", "", "", tempContextUrl);
//            if ("success".equals(jsonObjec.get("code"))) {
//                String balance = jsonObjec.getJSONObject("params").getString("result");
//                if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                    data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                    return data;
//                }
//            }
            JSONObject jsonObjec = c.openUserCommonAPI(ag_username, "open.balance.get", "", "", tempContextUrl);
            if ("success".equals(jsonObjec.get("code"))) {
                data.put("balance", JSONObject.fromObject(jsonObjec.get("params")).get("result"));
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
