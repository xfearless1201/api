package com.cn.tianxia.api.service.v2;

import java.util.Date;

/**
 * @Author: zed
 * @Date: 2019/5/26 10:45
 * @Description: 游戏数据
 */
public interface GameBetInfoService {
    Double selectUserValidBetAmuontList(Integer uid, Date begintime, Date endtime);
}
