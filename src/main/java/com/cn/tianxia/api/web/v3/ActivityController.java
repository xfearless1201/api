package com.cn.tianxia.api.web.v3;

import com.cn.tianxia.api.base.annotation.LogApi;
import com.cn.tianxia.api.common.v2.CacheKeyConstants;
import com.cn.tianxia.api.common.v2.ResultResponse;
import com.cn.tianxia.api.po.v2.GGLActivityPO;
import com.cn.tianxia.api.service.v2.ActivityService;
import com.cn.tianxia.api.utils.RedisUtils;
import com.cn.tianxia.api.web.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.text.DecimalFormat;
import java.util.Map;

/**
 * 
 * @ClassName ActivityController
 * @Description 活动接口
 * @author Hardy
 * @Date 2019年3月12日 上午11:21:59
 * @version 1.0.0
 */
@RequestMapping("V2/activity")
@Controller
public class ActivityController extends BaseController{
    //日志
    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);
    
    //活动
    @Autowired
    private ActivityService activityService;
    @Autowired
    private RedisUtils redisUtils;
    
    /**
     * 
     * @Description 刮刮乐活动
     * @param request
     * @param response
     * @param agentCode
     * @return
     */
    @LogApi("刮刮乐活动接口")
    @RequestMapping("/guaguale")
    @ResponseBody
    public ResultResponse guagualeActivity(HttpServletRequest request,HttpServletResponse response,String agentCode,String type){
        logger.info("调用刮刮乐获取接口开始=====================START======================");
        try {
            Map<String,String> userMap  = getUserInfoMap(redisUtils, request);
            if (null == userMap) {
                return ResultResponse.faild("用户未登录，请先登录");
            }
            if(StringUtils.isBlank(agentCode)){
                logger.info("请求参数错误:平台编码不能为空");
                return ResultResponse.faild("请求参数错误:平台编码不能为空");
            }
            ResultResponse result = activityService.guagualeAcitivity(userMap,agentCode,type);
            if("0".equals(result.getCode())){
                GGLActivityPO gglActivityPO = (GGLActivityPO) result.getData();
                //把用户刮到的金额放入缓存
                if (redisUtils.hasKey(CacheKeyConstants.CAGENT_GGL_UID + userMap.get("uid"))) {
                    String amount = redisUtils.get(CacheKeyConstants.CAGENT_GGL_UID + userMap.get("uid"));
                    if (StringUtils.isNotBlank(amount)){
                        gglActivityPO.setUsermoney(Double.valueOf(amount));
                        result.setData(gglActivityPO);
                    }
                } else {
                    String usermoney = new DecimalFormat("0.00").format(gglActivityPO.getUsermoney());
                    redisUtils.set(CacheKeyConstants.CAGENT_GGL_UID + userMap.get("uid"), usermoney,3*24*60*60);
                }
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("调用刮刮乐获取接口异常:{}",e.getMessage());
            return ResultResponse.error("调用刮刮乐获取接口异常,请联系技术");
        }
    }
}

