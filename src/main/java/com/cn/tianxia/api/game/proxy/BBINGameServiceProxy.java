package com.cn.tianxia.api.game.proxy;

import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.BBINGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @Auther: zed
 * @Date: 2019/3/8 14:13
 * @Description: BBIN游戏接口代理类
 */
@Service("BBIN")
public class BBINGameServiceProxy implements GameInterfaceService {

    private static final Logger logger = LoggerFactory.getLogger(BBINGameServiceProxy.class);

    @Override
    public JSONObject transferIn(GameTransferVO transfer) throws Exception {

        Map<String,String> data = transfer.getConfig();
        String ag_username = transfer.getAg_username();
        String billno = transfer.getBillno();
        String credit = transfer.getMoney();
        String username = transfer.getUsername();
        String ag_password = transfer.getPassword();
        String type = transfer.getType();

        BBINGameServiceImpl gameService = new BBINGameServiceImpl(data);
        String msg = gameService.newTransfer(ag_username, ag_password, billno, "IN", credit);
        // 解析响应结果
        if (msg.equals("success")) return TransferResponse.transferSuccess();
        if (msg.equals("error"))   return TransferResponse.transferFaild();
        // 失败或没有收到返回值做轮询3次查询订单接口
        int polls = 0;
        while (true){
            Thread.sleep(3000);
            logger.info("用户:【"+username+"】,订单号:【"+billno+"】,操作天下平台向游戏平台:【"+type+"】转入金额业务(游戏上分)业务,第{}次轮询转账订单次数", polls);
            polls++;
            boolean isCheck = gameService.newCheckTransfer(billno, ag_username);
            if (isCheck) {
                // 成功
                return TransferResponse.transferSuccess();
            } else {
                if (polls > 2) {
                    // 转账订单处理失败
                    return TransferResponse.transferFaild();
                }
            }

        }
    }

    @Override
    public JSONObject transferOut(GameTransferVO transfer) throws Exception {
        Map<String,String> data = transfer.getConfig();
        String ag_username = transfer.getAg_username();
        String billno = transfer.getBillno();
        String credit = transfer.getMoney();
        String username = transfer.getUsername();
        String ag_password = transfer.getPassword();
        String type = transfer.getType();
        BBINGameServiceImpl b = new BBINGameServiceImpl(data);
        String msg = b.newTransfer(ag_username, ag_password, billno, "OUT", credit + "");
        if (msg.equals("error"))   return TransferResponse.transferFaild();
        if ("success".equals(msg.toLowerCase())) {
            return TransferResponse.transferSuccess();
        } else {
            // 失败订单延时10秒查询订单状态
            int polls = 0;
            while (true){
                Thread.sleep(1500);
                logger.info("用户【"+username+"】,订单号:【"+billno+"】,游戏平台【" + type + "】向天下平台转入金额业务(游戏下分)业务,第{}次轮询转账订单次数", polls);
                polls++;
                boolean isCheck = b.checkTransfer(billno, ag_username);
                if (isCheck) {
                    // 成功
                    return TransferResponse.transferSuccess();
                } else {
                    if (polls > 2) {
                        // 转账订单处理失败
                        return TransferResponse.transferFaild();
                    }
                }
            }
        }
    }

    @Override
    public JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        JSONObject data = new JSONObject();
        data.put("type", "BBIN");
        try {
            Map<String,String> pmap = gameBalanceVO.getConfig();
            String ag_username = gameBalanceVO.getGamename();
            String ag_password = gameBalanceVO.getPassword();
            BBINGameServiceImpl bbinService = new BBINGameServiceImpl(pmap);
//            String msg = bbinService.CheckUsrBalance(ag_username, ag_password);
//            logger.info("用户查询BBIN游戏余额接口响应结果:{}",msg);
//            JSONObject jsonObject = JSONObject.fromObject(msg);
//            if(jsonObject.getString("result").equals("true")){
//                JSONArray jsonArray = JSONArray.fromObject(jsonObject.getString("data"));
//                JSONObject balanceData = jsonArray.getJSONObject(0);
//                if(balanceData != null && balanceData.containsKey("balance")){
//                    String balance = jsonArray.getJSONObject(0).getString("balance");
//                    if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
//                        data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
//                        return data;
//                    }
//                }
//            }
            
            String msg = bbinService.CheckUsrBalance(ag_username, ag_password);
            JSONObject json;
            json = JSONObject.fromObject(msg);
            if ("true".equals(json.get("result").toString())) {
                JSONArray jsonArray = JSONArray.fromObject(json.getString("data"));
                json = jsonArray.getJSONObject(0);
                data.put("balance", json.get("Balance").toString());
            } else {
                data.put("balance", "维护中");
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.put("balance", "0.00");
        }

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
