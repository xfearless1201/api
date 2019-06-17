package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.PTGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:20
 * @Description: PT游戏接口代理类
 */
@Service("PT")
public class PTGameServiceProxy implements GameInterfaceService {
    
    private static final Logger logger = LoggerFactory.getLogger(PTGameServiceProxy.class);
    
    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        PTGameServiceImpl p = new PTGameServiceImpl(gameTransferVO.getConfig());
        String msg = p.Deposit(gameTransferVO.getAg_username(), gameTransferVO.getMoney(), gameTransferVO.getBillno());
        if (StringUtils.isBlank(msg)) {
            // 异常订单
            return TransferResponse.transferProcess();
        }
        JSONObject json = JSONObject.fromObject(msg);
        if (json.containsKey("result") && json.getString("result").indexOf("errorcode") < 0) {
            // 成功
            return TransferResponse.transferSuccess();
        }
        return TransferResponse.transferFaild();
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        String msg = "";
        PTGameServiceImpl p = new PTGameServiceImpl(gameTransferVO.getConfig());
        msg = p.Withdraw(gameTransferVO.getAg_username(), gameTransferVO.getMoney(), gameTransferVO.getBillno());
        JSONObject jsonObject = JSONObject.fromObject(msg);
        msg = jsonObject.getJSONObject("result").getString("result");
        if (msg.indexOf("errorcode") < 0) {
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
        data.put("type", "PT");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String ag_password = gameBalanceVO.getPassword();
            PTGameServiceImpl p = new PTGameServiceImpl(pmap);
            String response = p.GetPlayerInfo(ag_username);
            logger.info("用户查询PT电子游戏响应结果:{}",response);
//            if(StringUtils.isNotEmpty(response)){
//                JSONObject json = JSONObject.fromObject(response);
//                json = json.getJSONObject("result");
//                if (json == null || "".equals(json.toString())) {
//                    data.put("balance", "0.00");
//                    return data;
//                } else {
//                    String balance = json.getString("BALANCE").toString();
//                    if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                        data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                        return data;
//                    }
//                }
//            }
            JSONObject json = JSONObject.fromObject(p.GetPlayerInfo(ag_username));
            json = json.getJSONObject("result");
            if (json == null || "".equals(json.toString())) {
                data.put("balance", "维护中");
                return data;
            } else {
                String balance = json.getString("BALANCE").toString();
                data.put("balance", balance);
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
