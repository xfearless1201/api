package com.cn.tianxia.api.game;

import com.alibaba.fastjson.JSONObject;
import com.cn.tianxia.api.vo.v2.*;

/**
 *
 * @ClassName GameFactoryService
 * @Description 游戏接口
 * @author Jacky
 * @Date 2019年5月18日 下午4:31:46
 * @version 1.0.0
 */
public interface GameFactoryService {

    /**
     *
     * @Description 上分接口
     * @param gameTransferVO
     * @return
     */
    String transferIn(GameTransferVO gameTransferVO) throws Exception;


    /**
     *
     * @Description 下分接口
     * @param gameTransferVO
     * @return
     */
    String transferOut(GameTransferVO gameTransferVO) throws Exception;

    /**
     *
     * @Description 游戏跳转
     * @return
     */
    JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception;

    /**
     * 获取游戏余额
     * @Description
     * @return
     */
    String  getBalance(GameBalanceVO gameBalanceVO) throws Exception;

    /**
     *
     * @Description 检查或创建用户信息
     * @return
     */
    String checkOrCreateAccount(GameForwardVO gameForwardVO) throws Exception;

    /**
     *
     * @Description 查询订单
     * @return
     */
    JSONObject queryTransferOrder(GameQueryOrderVO gameQueryOrderVO) throws Exception;
}
