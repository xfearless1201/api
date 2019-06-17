package com.cn.tianxia.api.web.v3;

import cn.hutool.core.lang.UUID;
import com.cn.tianxia.api.base.annotation.LogApi;
import com.cn.tianxia.api.common.v2.CacheKeyConstants;
import com.cn.tianxia.api.common.v2.ResultResponse;
import com.cn.tianxia.api.service.v2.ActivityService;
import com.cn.tianxia.api.utils.RedisUtils;
import com.cn.tianxia.api.web.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Hardy
 * @version 1.0.0
 * @ClassName ReceiveActivityBonusController
 * @Description 领取活动奖金接口
 * @Date 2019年3月14日 下午12:17:26
 */
@RestController
@RequestMapping("/V2/receive/bonus")
public class ReceiveActivityBonusController extends BaseController {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(ReceiveActivityBonusController.class);

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RedisUtils redisUtils;


    /**
     * @param request
     * @param response
     * @param activityAmount 活动金额
     * @param activityId     活动ID
     * @param code           手机验证码
     * @param phoneNo        手机号码
     * @return
     * @Description 领取刮刮乐奖金
     */
    @LogApi("刮刮乐领取奖金接口")
    @PostMapping("/ggl")
    public ResultResponse receiveGGLBonus(HttpServletRequest request, HttpServletResponse response,
                                          String activityAmount, String activityId, String code, String phoneNo) {
        logger.info("领取刮刮乐奖金请求参数:活动金额" + activityAmount + "活动ID" + activityId + "手机号码" + phoneNo + "手机验证码" + code);
        //判断请求参数
        if (StringUtils.isBlank(activityId)) {
            return ResultResponse.faild("请求参数异常,活动ID不能为空");
        }

        if (StringUtils.isBlank(activityAmount)) {
            return ResultResponse.faild("请求参数异常,活动金额不能为空");
        }
        
        //从缓存中获取用户ID
        Map<String, String> data = getUserInfoMap(redisUtils, request);
        if (CollectionUtils.isEmpty(data)) {
            logger.info("用户未登录,请重新登录");
            return ResultResponse.faild("用户未登录,请重新登录");
        }
        
        String lockKey = CacheKeyConstants.CAGENT_GGL_LOCK_UID + data.get("uid");
        //唯一标识
        String uuid = UUID.randomUUID().toString();
        try {
            boolean hasLock = redisUtils.hasLock(lockKey, uuid, 60);
            if (hasLock) {
                return ResultResponse.faild("领取请求已发送,请勿重复发送....");
            }
            //从缓存中获取用户的刮奖金额
            Double gglAmount = Double.valueOf(redisUtils.get(CacheKeyConstants.CAGENT_GGL_UID + data.get("uid")));
            if (gglAmount == null) {
                return ResultResponse.faild("获取用户刮奖金额为空");
            }
            logger.info("用户:" + phoneNo + "的刮奖金额为:" + gglAmount);

            //活动请求金额
            Double receiveAmount = Double.parseDouble(activityAmount);
            if (Double.compare(receiveAmount, gglAmount) != 0) {
                return ResultResponse.faild("请求参数【活动金额】与刮奖金额不符合");
            }
            ResultResponse result = activityService.receiveGGLBonus(data, activityAmount, activityId, code, phoneNo);
            if ("0".equals(result.getCode())) {
                //领取成功
                redisUtils.delete(CacheKeyConstants.CAGENT_GGL_UID + data.get("uid"));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("调用领取刮刮乐活动奖金接口异常:{}", e.getMessage());
            return ResultResponse.faild("领取失败");
        } finally {
            //释放锁
            redisUtils.releaseLock(lockKey, uuid);
        }
    }

}
