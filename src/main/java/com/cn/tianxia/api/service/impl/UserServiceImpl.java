package com.cn.tianxia.api.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cn.tianxia.api.domain.txdata.UserDao;
import com.cn.tianxia.api.service.UserService;


/**
 * 功能概要：UserService实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserDao userDao;
    
    protected Logger logger = LoggerFactory.getLogger(this.getClass());


    @Override
    public List<Map<String, String>> selectUserGameStatus(Map<String, Object> map) {
        return userDao.selectUserGameStatus(map);
    }

    @Override
    public void insertUserGameStatus(Map<String, Object> map) {
        userDao.insertUserGameStatus(map);
    }


    @Override
    public List<Map<String, String>> selectTcagentYsepay(String paymentName) {
        return userDao.selectTcagentYsepay(paymentName);
    }

    @Override
    public Map<String, Object> selectUserTypeHandicap(String game, String uid) {
        return userDao.selectUserTypeHandicap(game, uid);
    }

    @Override
    public int insertPSToken(String auth, int step, String uid) {
        return userDao.insertPSToken(auth, step, uid);
    }

    @Override
    public Map<String, String> selectPSByauth(String auth) {
        return userDao.selectPSByauth(auth);
    }

    @Override
    public void UpdatePSToken(String auth, int step) {
        userDao.UpdatePSToken(auth, step);
    }

}
