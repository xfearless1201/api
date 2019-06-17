package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.CQJServiceimpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;
import com.cn.tianxia.api.vo.v2.TransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 11:27
 * @Description: CQ9游戏接口代理类
 */
@Service("CQJ")
public class CQJGameServiceProxy implements GameInterfaceService {
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        Map<String,String> data = gameTransferVO.getConfig();
        String ag_username = gameTransferVO.getAg_username();
        String billno = gameTransferVO.getBillno();
        String credit = gameTransferVO.getMoney();
        String ag_password = gameTransferVO.getPassword();
        CQJServiceimpl cqjServiceimpl = new CQJServiceimpl(data);
        TransferVO transferVO = new TransferVO();
        transferVO.setPassword(ag_password);
        transferVO.setAccount(ag_username);
        transferVO.setOrderNo(billno);
        transferVO.setMoney(Double.valueOf(credit));
        String returnMsg = cqjServiceimpl.transferIn(transferVO);
        if ("success".equalsIgnoreCase(returnMsg)) {
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
        String ag_password = gameTransferVO.getPassword();
        CQJServiceimpl cqjServiceimpl = new CQJServiceimpl(data);
        TransferVO transferVO = new TransferVO();
        transferVO.setPassword(ag_password);
        transferVO.setAccount(ag_username);
        transferVO.setOrderNo(billno);
        transferVO.setMoney(Double.valueOf(credit));
        String returnMsg = cqjServiceimpl.transferOut(transferVO);
        if ("success".equalsIgnoreCase(returnMsg)) {
            // 转账订单提交成功
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
        data.put("type", "CQJ");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String ag_password = gameBalanceVO.getPassword();
            CQJServiceimpl cqj = new CQJServiceimpl(pmap);
//            String str = cqj.findAccount(ag_username);
//            if(!str.equalsIgnoreCase("error") && PatternUtils.isMatch(str, PatternUtils.MONEYREGEX)){
//                data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(str)));
//                return data;
//            }
            
            String str = cqj.findAccount(ag_username);
            if ("error".equals(str)) {
                data.put("balance", "维护中");
            } else {
                data.put("balance", str);
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

    /**
     * @Description 封装返回结果
     * @param code
     * @param message
     * @return
     */
    private JSONObject transferResponse(String code, String message) {
        JSONObject data = new JSONObject();
        if (StringUtils.isBlank(code) || StringUtils.isBlank(message)) {
            data.put("msg", "error");
            data.put("errmsg", "转账异常");
            data.put("status", "error");
        } else {
            data.put("msg", code);
            data.put("errmsg", message);
            data.put("status", "error");
        }
        return data;
    }
}
