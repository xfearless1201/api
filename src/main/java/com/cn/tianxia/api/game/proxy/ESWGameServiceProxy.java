package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.ESWServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:48
 * @Description: ESW游戏接口代理类
 */
@Service("ESW")
public class ESWGameServiceProxy implements GameInterfaceService {

    private static final Logger logger = LoggerFactory.getLogger(ESWGameServiceProxy.class);

    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        Map<String,String> data = gameTransferVO.getConfig();
        String ag_username = gameTransferVO.getAg_username();
        String billno = gameTransferVO.getBillno();
        String credit = gameTransferVO.getMoney();
        ESWServiceImpl eSWServiceImpl = new ESWServiceImpl(data);
        String returnMsg = eSWServiceImpl.transferIn(ag_username,String.valueOf(credit),billno);
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
        ESWServiceImpl eSWServiceImpl = new ESWServiceImpl(data);
        String returnMsg = eSWServiceImpl.transferOut(ag_username,String.valueOf(credit),billno);
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
        JSONObject result = new JSONObject();
        result.put("type", "ESW");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String ag_password = gameBalanceVO.getPassword();
            ESWServiceImpl eswService = new ESWServiceImpl(pmap);
//            String msg = eswService.queryUserInfo(ag_username);
//            JSONObject jsonObject = JSONObject.fromObject(msg);
//            if (jsonObject.getInt("code") == 0) {
//                String balance = jsonObject.getString("money");
//                if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){                    
//                    data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                    return data;
//                }
//            }
            
            String data = eswService.queryUserInfo(ag_username);
            JSONObject jsonObject = JSONObject.fromObject(data);
            if (jsonObject.getInt("code") == 0) {
                result.put("balance", jsonObject.getString("money"));
            } else {
                result.put("balance", "维护中");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.put("balance", "0.00");
        return result;
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
