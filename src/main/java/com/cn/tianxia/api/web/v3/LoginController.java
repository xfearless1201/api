package com.cn.tianxia.api.web.v3;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cn.tianxia.api.common.v2.CacheKeyConstants;
import com.cn.tianxia.api.error.BusinessErrorEnum;
import com.cn.tianxia.api.error.BusinessException;
import com.cn.tianxia.api.po.v2.ResultResponse;
import com.cn.tianxia.api.service.v2.UserLoginService;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.RedisUtils;
import com.cn.tianxia.api.utils.RequestResponseUtil;
import com.cn.tianxia.api.web.BaseController;

import cn.hutool.core.lang.UUID;

/**
 * 
 * @ClassName LoginController
 * @Description 登录接口
 * @author Hardy
 * @Date 2019年5月13日 下午12:10:02
 * @version 1.0.0
 */
@RestController
//@RequestMapping("v2")
public class LoginController extends BaseController{
    
    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private UserLoginService userLoginService;
    
    //测试分布式锁
//    @PostMapping("demo/account/login")
    public ResultResponse accountLoginDemo(String username,String password){
        String lockKey = CacheKeyConstants.DISTRIBUTED_LOCK + username;
        String requestId = UUID.randomUUID().toString();
        try {
            boolean hasLock = redisUtils.hasLock(lockKey,requestId,2);
            if(hasLock){
                //存在锁
                return ResultResponse.faild("请求已发送,请不要重复提交");
            }
            
            System.err.println("执行业务完成=====================end======================");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            //释放锁
//            redisUtils.releaseLock(lockKey, requestId);
        }
        return ResultResponse.success();
    }
    
    
    /**
     * 
     * @Description 账号登录接口
     * @param request
     * @param response
     * @return
     */
//    @PostMapping("/account/login")
    public ResultResponse accountLogin(HttpServletRequest request,HttpServletResponse response){
        try {
            Map<String,String> usermap = RequestResponseUtil.getRequestBodyMap(request);
            if(CollectionUtils.isEmpty(usermap)) return ResultResponse.faild("请求参数不能为空");
            String sessionId = request.getSession().getId();
            //校验请求参数
            usermap.put("sessionId", request.getSession().getId());
            vieryParams(usermap);
            //登录账号
            String username = usermap.get("username");
            String password = usermap.get("password");
            String isMobile = usermap.get("isMobile");
            //获取请求地址.域名.ip
            String address = IPTools.getIpCnAddress(request);
            String refurl = request.getHeader("referer");
            String ip = IPTools.getIp(request);
            //登录
//            return userLoginService.accountLogin(username, password, isMobile,refurl,ip,address,sessionId);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("登录异常:{}",e.getMessage());
            return ResultResponse.faild(e.getMessage());
        }
    }
    
    /**
     * 
     * @Description 校验请求参数
     * @param data
     * @return
     */
    private void vieryParams(Map<String,String> data) throws BusinessException{
        
        if(!data.containsKey("username") || StringUtils.isEmpty(data.get("username"))){
            throw new BusinessException(BusinessErrorEnum.REQUEST_PARAMS_ERROR.setErrMsg("用户名不能为空"));
        }
        
        if(!data.containsKey("password") || StringUtils.isEmpty(data.get("password"))){
            throw new BusinessException(BusinessErrorEnum.REQUEST_PARAMS_ERROR.setErrMsg("用户密码不能为空"));
        }
        
        if(data.containsKey("isCaptcha") && StringUtils.isNoneEmpty(data.get("isCaptcha"))
                && data.get("isCaptcha").equals("1")){
            //校验验证码
            if(data.containsKey("captcha") && StringUtils.isNoneEmpty(data.get("captcha"))){
                //从缓存中获取验证码
                String validCode = redisUtils.get(CacheKeyConstants.VALIDATE_CODE_SESSION+data.get("sessionId"));
                if(data.get("captcha").equals(validCode)){
                    return;
                }
            }
        }
        throw new BusinessException(BusinessErrorEnum.REQUEST_PARAMS_ERROR.setErrMsg("验证码输入错误"));
    }
}
