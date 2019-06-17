package com.cn.tianxia.api.web.v3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cn.tianxia.api.common.v2.CacheKeyConstants;
import com.cn.tianxia.api.common.v2.KeyConstant;
import com.cn.tianxia.api.common.v2.ResultResponse;
import com.cn.tianxia.api.service.v2.BalanceService;
import com.cn.tianxia.api.service.v2.UserGameTransferService;
import com.cn.tianxia.api.utils.DESEncrypt;
import com.cn.tianxia.api.utils.IPTools;
import com.cn.tianxia.api.utils.RedisUtils;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;
import com.cn.tianxia.api.web.BaseController;

import cn.hutool.core.lang.UUID;
import net.sf.json.JSONObject;

/**
 * 
 * @ClassName BalanceController
 * @Description 余额接口
 * @author Hardy
 * @Date 2019年6月2日 下午10:12:52
 * @version 1.0.0
 */
@RestController
@RequestMapping("V2/balance")
public class BalanceController extends BaseController{
    
    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private BalanceService balanceService;
    
    @Autowired
    private UserGameTransferService userGameTransferService;
    /**
     * 
     * @Description 获取用户的所有余额
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/all/{types}")
    public ResultResponse getUserAllBalance(HttpServletRequest request,HttpServletResponse response,@PathVariable(name="types",required=true) String types){
        //校验用户请求参数
        if(StringUtils.isBlank(types)){
            return ResultResponse.faild("请求参数异常,查询余额类型不能为空");
        }
        //获取用户信息
        Map<String,String> data = getUserInfoMap(redisUtils, request);
        if(CollectionUtils.isEmpty(data)){
            return ResultResponse.faild("用户未登录,无权访问");
        }
        //用户账号
        String username = data.get("userName");
        //获取用户ID
        String uid = data.get("uid");
        if(StringUtils.isBlank(uid)){
            logger.info("用户【"+username+"】从缓存中查询用户ID失败:{}",uid);
            return ResultResponse.faild("获取用户信息异常,请联系客服!"); 
        }
        
        String ag_password = data.get("ag_password");
        String ag_username = data.get("ag_username");
        String hg_username = data.get("hg_username");
        String cagent = data.get("cagent");
        String ip = IPTools.getIp(request);
        StringBuffer url = request.getRequestURL();
        String tempContextUrl = url.delete(url.length() - request.getRequestURI().length(), url.length())
                .append("/").toString();
        //生成key,防止用户重复提交
        String lockKey = CacheKeyConstants.QUERY_USER_ALL_BALANCE_KEY+uid;
        //生成唯一标示
        String uuid = UUID.randomUUID().toString();
        try {
            boolean hasLock = redisUtils.hasLock(lockKey, uuid, 60);
            if(hasLock){
                return ResultResponse.faild("查询余额请求已提交,请无重复操作...."); 
            }
            
            //封装查询余额类型列表
            Set<String> set = new HashSet<String>();
            //切割请求类型
            List<String> list = Arrays.asList(types.split(","));
            if(CollectionUtils.isEmpty(list)){
                logger.info("用户【"+username+"】查询所有余额请求参数异常:{}",types);
                return ResultResponse.faild("请求参数异常,查询余额类型不能为空");
            }
            
            list.stream().forEach(item->{
                if(StringUtils.isNotBlank(item)){
                    set.add(item.trim());
                }
            });
            
            Map<String, String> pmap = formatPlatformConfigByCagent(cagent.toUpperCase());
            if (CollectionUtils.isEmpty(pmap)) {
                logger.info("查询平台配置信息为空");
                return ResultResponse.faild("请求参数异常,查询平台游戏配置信息异常");
            }
            GameBalanceVO gameBalanceVO = new GameBalanceVO();
            gameBalanceVO.setGamename(ag_username);
            gameBalanceVO.setHg_username(hg_username);
            gameBalanceVO.setIp(ip);
            gameBalanceVO.setTempContextUrl(tempContextUrl);
            gameBalanceVO.setUid(uid);
            // 解密密码
            DESEncrypt d = new DESEncrypt(KeyConstant.DESKEY);
            ag_password = d.decrypt(ag_password);
            gameBalanceVO.setPassword(ag_password);
            gameBalanceVO.setConfig(pmap);
            //发起查询请求,通过多线程处理
            return balanceService.getUserAllBalance(gameBalanceVO,set);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("调用获取用户的所有余额接口异常:{}",e.getMessage());
            return ResultResponse.faild("查询用户有所余额异常");
        }finally {
            //释放锁
            redisUtils.releaseLock(lockKey, uuid);
        }
    }
    
    
    private Map<String,String> formatPlatformConfigByCagent(String cagent){
        Map<String,String> data = new HashMap<>();
        try {
            Map<String,String> pmap = userGameTransferService.getPlatformConfig();
            if(!CollectionUtils.isEmpty(pmap)){
                //通过平台编码获取配置信息
                Iterator<String> iterator = pmap.keySet().iterator();
                while(iterator.hasNext()){
                    String key = iterator.next();
                    String val = pmap.get(key);
                    if(StringUtils.isNotBlank(val)){
                        //解析配置信息
                        JSONObject jsonObject = JSONObject.fromObject(val);
                        if(jsonObject.containsKey(cagent)){
                            val = jsonObject.getString(cagent);
                        }else if(jsonObject.containsKey("ALL")){
                            val = jsonObject.getString("ALL");
                        }
                        data.put(key,val);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("获取平台游戏配置信息异常:{}",e.getMessage());
        }
        return data;
    }
}
