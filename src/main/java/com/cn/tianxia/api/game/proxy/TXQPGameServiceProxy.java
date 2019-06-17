/****************************************************************** 
 *
 *    Powered By tianxia-online. 
 *
 *    Copyright (c) 2018-2020 Digital Telemedia 天下科技 
 *    http://www.d-telemedia.com/ 
 *
 *    Package:     com.cn.tianxia.api.game.proxy 
 *
 *    Filename:    TXQPGameServiceProxy.java 
 *
 *    Description: TODO(用一句话描述该文件做什么) 
 *
 *    Copyright:   Copyright (c) 2018-2020 
 *
 *    Company:     天下科技 
 *
 *    @author:    Roman 
 *
 *    @version:    1.0.0 
 *
 *    Create at:   2019年05月31日 17:16 
 *
 *    Revision: 
 *
 *    2019/5/31 17:16 
 *        - first revision 
 *
 *****************************************************************/

package com.cn.tianxia.api.game.proxy;


import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.impl.TXQPGameServiceImpl;
import com.cn.tianxia.api.po.v2.TransferResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.vo.v2.GameCheckOrCreateVO;
import com.cn.tianxia.api.vo.v2.GameForwardVO;
import com.cn.tianxia.api.vo.v2.GameQueryOrderVO;
import com.cn.tianxia.api.vo.v2.GameTransferVO;

import net.sf.json.JSONObject;

/**
 *  * @ClassName TXQPGameServiceProxy
 *  * @Description TODO(天下棋牌游戏接口代理类)
 *  * @Author Roman
 *  * @Date 2019年05月31日 17:16
 *  * @Version 1.0.0
 *  
 **/
@Service("TXQP")
public class TXQPGameServiceProxy implements GameInterfaceService {

    private static final String SUCCESS = "success";
    private static final String FAILED = "failed";

    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        TXQPGameServiceImpl txqpGameService = new TXQPGameServiceImpl(gameTransferVO.getConfig());
        String status = txqpGameService.transferIn(gameTransferVO);
        if (status.equals(SUCCESS)) {
            return TransferResponse.transferSuccess();
        }
        if (status.equals(FAILED)) {
            return TransferResponse.transferFaild();
        }
        return TransferResponse.transferProcess();
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        TXQPGameServiceImpl txqpGameService = new TXQPGameServiceImpl(gameTransferVO.getConfig());
        String status = txqpGameService.transferOut(gameTransferVO);
        if (status.equals(SUCCESS)) {
            return TransferResponse.transferSuccess();
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
        try {
            TXQPGameServiceImpl txqpGameService = new TXQPGameServiceImpl(gameBalanceVO.getConfig());
            String balance = txqpGameService.getBalance(gameBalanceVO.getGamename());
            if(StringUtils.isNotEmpty(balance) && PatternUtils.isMatch(balance, PatternUtils.MONEYREGEX)){
                data.put("balance", new DecimalFormat("0.00").format(Double.parseDouble(balance)));
                return data;
            }
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
