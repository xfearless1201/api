package com.cn.tianxia.api.po.v2;

import net.sf.json.JSONObject;

/**
 * 
 * @ClassName BaseGameResponse
 * @Description 游戏返回结果封装类 
 * @author Hardy
 * @Date 2019年6月10日 下午3:37:14
 * @version 1.0.0
 */
public class BaseGameResponse {

    public static JSONObject error(String msg,String errmsg){
        JSONObject data = new JSONObject();
        data.put("status", "error");
        data.put("msg",msg);
        data.put("errmsg",errmsg);
        return data;
    }
}
