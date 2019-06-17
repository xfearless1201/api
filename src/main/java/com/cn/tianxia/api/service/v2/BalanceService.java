package com.cn.tianxia.api.service.v2;

import java.util.Set;

import com.cn.tianxia.api.common.v2.ResultResponse;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;

/**
 * 
 * @ClassName BalanceService
 * @Description 余额接口
 * @author Hardy
 * @Date 2019年6月2日 下午10:36:04
 * @version 1.0.0
 */
public interface BalanceService {

    
    /**
     * 
     * @Description 获取用户所有余额接口
     * @param uid
     * @param types
     * @return
     */
    public ResultResponse getUserAllBalance(GameBalanceVO gameBalanceVO,Set<String> types) throws Exception;
}
