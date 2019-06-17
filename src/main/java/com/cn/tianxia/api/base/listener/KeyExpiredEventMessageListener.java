package com.cn.tianxia.api.base.listener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.cn.tianxia.api.common.v2.CacheKeyConstants;
import com.cn.tianxia.api.project.v2.OnlineUserEntity;
import com.cn.tianxia.api.service.v2.OnlineUserService;
import com.cn.tianxia.api.utils.SpringContextUtils;

/**
 * @ClassName KeyExpiredEventMessageListener
 * @Description redis失效事件
 * @author Hardy
 * @Date 2019年5月20日 下午2:53:33
 * @version 1.0.0
 */
@Component
public class KeyExpiredEventMessageListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expired = message.toString();
        String onlineKey = CacheKeyConstants.ONLINE_USER_KEY_UID;
        if (expired.contains(onlineKey)) {
            String uid = expired.replace(CacheKeyConstants.ONLINE_USER_KEY_UID, "");
            if (StringUtils.isNoneEmpty(uid)) {
                OnlineUserService onlineUserService = (OnlineUserService) SpringContextUtils
                        .getBeanByClass(OnlineUserService.class);
                OnlineUserEntity onlineUser = onlineUserService.getByUid(uid);
                if (onlineUser != null) {
                    onlineUser.setLogoutTime(System.currentTimeMillis());
                    onlineUser.setOffStatus((byte) 0);
                    onlineUser.setIsOff((byte) 1);
                    onlineUser.setUid(Long.parseLong(uid));
                    onlineUserService.insertOrUpdateOnlineUser(onlineUser);
                }
            }
        }
    }

}
