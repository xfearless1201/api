package com.cn.tianxia.api.service;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.springframework.context.annotation.Scope;

/**
 * 功能概要：UserService接口类
 * 
 */
public interface UserService {


	List<Map<String, String>> selectUserGameStatus(Map<String, Object> map);

	void insertUserGameStatus(Map<String, Object> map);

	List<Map<String, String>> selectTcagentYsepay(String paymentName);

	// 查询用户分层盘口设置
	@Scope("prototype")
	Map<String, Object> selectUserTypeHandicap(@Param("game") String game, @Param("uid") String uid);

    //保存pstaoken
    int insertPSToken(@Param("auth") String auth,@Param("step") int step,@Param("uid") String uid);
    
    //PS游戏查询token
    Map<String, String> selectPSByauth(@Param("auth") String auth);
    
    //PS游戏token状态
    void UpdatePSToken(@Param("auth") String auth,@Param("step") int step);


}
