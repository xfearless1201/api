package com.cn.tianxia.api.service.impl;

import java.util.concurrent.Callable;

import com.alibaba.fastjson.JSONObject;
import com.cn.tianxia.api.service.LuckyDrawService;

/**
 * @Auther: zed
 * @Date: 2019/3/24 11:12
 * @Description: 异步调用方法
 */
public class LuckyDrawCallback implements Callable<JSONObject> {

    private LuckyDrawService luckyDrawService;
    private String userName;
    private String refurl;

    public LuckyDrawCallback(LuckyDrawService luckyDrawService,String userName,String refurl){
        this.luckyDrawService = luckyDrawService;
        this.userName = userName;
        this.refurl = refurl;
    }

    @Override
    public JSONObject call() throws Exception {
        return luckyDrawService.luckyDraw(userName, refurl);
    }
}
