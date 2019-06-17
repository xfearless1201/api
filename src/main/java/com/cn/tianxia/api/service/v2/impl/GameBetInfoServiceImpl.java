package com.cn.tianxia.api.service.v2.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.domain.ftpdata.GameBetInfoDao;
import com.cn.tianxia.api.service.v2.GameBetInfoService;

/**
 * @Author: zed
 * @Date: 2019/5/26 10:47
 * @Description: 游戏注单信息记录
 */
@Service
public class GameBetInfoServiceImpl implements GameBetInfoService {

    @Autowired
    private GameBetInfoDao gameBetInfoDao;

    @Override
    public Double selectUserValidBetAmuontList(Integer uid, Date begintime, Date endtime) {
        return gameBetInfoDao.selectUserValidBetAmuontList(uid, begintime, endtime);
    }
}
