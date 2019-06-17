package com.cn.tianxia.api.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cn.tianxia.api.base.annotation.LogApi;
import com.cn.tianxia.api.common.ExpiredDateConsts;
import com.cn.tianxia.api.common.v2.BaseResponse;
import com.cn.tianxia.api.common.v2.CacheKeyConstants;
import com.cn.tianxia.api.common.v2.PatternUtils;
import com.cn.tianxia.api.service.LuckyDrawService;
import com.cn.tianxia.api.service.impl.LuckyDrawCallback;
import com.cn.tianxia.api.utils.RedisUtils;

@Controller
@RequestMapping("LuckyDraw")
public class LuckyDrawControl extends BaseController {

    private static ExecutorService cachedThreadExecutor = Executors.newFixedThreadPool(5);
    
    @Resource
    private LuckyDrawService luckyDrawService;
    @Autowired
    private RedisUtils redisUtils;

    /**
     * 获取当前网站活动状态
     *
     * @param request
     * @param response
     * @return
     */
    @LogApi("红包抽奖")
    @RequestMapping(value = "getPrize.do")
    @ResponseBody
    public JSONObject getPrize(HttpServletRequest request, HttpServletResponse response, String username) throws Exception {
        logger.info("获取当前网站用户【" + username + "LuckyDraw】活动状态开始=================start==================");
        //添加分布式锁
        String lockKey = CacheKeyConstants.ACTIVITY_LUCKDRAW_KEY_UID + username;
        String uuid = UUID.randomUUID().toString();
        boolean hasLock = redisUtils.hasLock(lockKey, uuid, ExpiredDateConsts.ACTIVITY_LUCKDRAW_EXPIRED_DATE);
        if (hasLock) {
            logger.info("用户" + username + "LuckyDraw抽奖频度太快...");
            return BaseResponse.faild("faild", "抽奖频度太快");
        }
        try {
            
            //获取请求域名
            String refurl = request.getHeader("referer");
            if(StringUtils.isBlank(refurl)){
                logger.info("获取当前网站用户【" + username + "请求域名失败:{}",refurl);
                return BaseResponse.faild("faild", "获取请求域名失败");
            }
            
            String domain = PatternUtils.getMatchDomain(refurl);
            if(StringUtils.isBlank(domain)){
                logger.info("获取当前网站用户【" + username + "请求域名失败:{}",refurl);
                return BaseResponse.faild("faild", "获取请求域名失败");
            }
            
            FutureTask<JSONObject> dbtask = new FutureTask<>(new LuckyDrawCallback(luckyDrawService, username, domain));
            cachedThreadExecutor.submit(dbtask);
            JSONObject result = dbtask.get();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("获取当前网站用户【" + username + "LuckyDraw】活动状态异常:{}", e.getMessage());
            return BaseResponse.faild("faild", "抽奖异常,请联系客服");
        } finally {
            redisUtils.releaseLock(lockKey, uuid);
            if(cachedThreadExecutor.isTerminated()){
                cachedThreadExecutor.shutdown();
            }
        }
    }

    /**
     * 获取当前网站活动状态
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "getStatus.do")
    @ResponseBody
    public Object LuckyDrawStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.info("获取当前网站用户活动状态开始=================start==================");
        JSONObject jo = new JSONObject();
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String refurl = request.getHeader("referer").split("/")[2];
            String domain = PatternUtils.getMatchDomain(request.getHeader("referer"));
            List<Map<String, Object>> list = luckyDrawService.selectLuckyDrawStatus(domain);
            logger.info("来源域名是【{}】", refurl);
            if (list.size() < 1) {
                jo.put("status", "faild");
                jo.put("msg", "暂无活动");
                return jo;
            }
            Map<String, Object> map = list.get(0);
            String now = df.format(new Date());
            String day = now.substring(0, 10);
            String begintime = day + " " + map.get("begintime").toString();
            String endtime = day + " " + map.get("endtime").toString();
            String name = map.get("luckyname").toString();
            String type = map.get("type").toString();
            String minamount = map.get("minamount").toString();
            String maxamount = map.get("maxamount").toString();
            //未到当日开奖时间
            long diff = df.parse(now).getTime() - df.parse(begintime).getTime();
            if (diff < 0) {
                jo.put("status", "waiting");
                jo.put("now", now);
                jo.put("begintime", begintime);
                jo.put("endtime", endtime);
                jo.put("diff", -diff / 1000);
                jo.put("msg", "未到活动时间");
                return jo;
            }
            //未到隔日开奖时间
            diff = df.parse(now).getTime() - df.parse(endtime).getTime();
            if (diff > 0) {
                Calendar c = Calendar.getInstance();
                c.setTime(df.parse(begintime));
                c.add(Calendar.DAY_OF_MONTH, 1);
                long dd = c.getTimeInMillis() - df.parse(now).getTime();

                jo.put("status", "waiting");
                jo.put("now", now);
                jo.put("begintime", begintime);
                jo.put("endtime", endtime);
                jo.put("diff", dd / 1000);
                jo.put("msg", "未到活动时间");
                return jo;
            }
            //活动正常开启
            jo.put("status", "success");
            jo.put("now", now);
            jo.put("begintime", begintime);
            jo.put("endtime", endtime);
            jo.put("diff", -diff / 1000);
            jo.put("msg", "正常");
            jo.put("name", name);
            jo.put("type", type);
            jo.put("minamount", minamount);
            jo.put("maxamount", maxamount);

            JSONArray data = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                JSONObject json = new JSONObject();
                json.put("balance", list.get(i).get("balance").toString());
                json.put("times", list.get(i).get("times").toString());
                data.add(json);
            }
            jo.put("data", data);
            return jo;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("调用红包抽奖接口异常:{}", e.getMessage());
            jo.put("status", "faild");
            jo.put("msg", "调用红包抽奖接口异常");
            return jo;
        }
    }
}  