package com.cn.tianxia.api.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cn.tianxia.api.common.v2.SystemConfigLoader;

@Controller
@RequestMapping("LoginMap")
@Scope("prototype")
public class LoginMapControl extends BaseController {

    @Autowired
    private SystemConfigLoader systemConfigLoader;
   

    @RequestMapping("/update.do")
    @ResponseBody
    public String test(String uid, String key, String sid) {
        String acckey = systemConfigLoader.getProperty("key");
        if (key == null || "".equals(key) || !key.equals(acckey)) {
            return "faild";
        }
        if (onlineMap.containsKey(uid)) {
            if (loginmaps.containsKey(uid)) {
                Map<String, String> loginmap = loginmaps.get(uid);
                loginmap.put("uid", uid);
                loginmap.put("sessionid", sid);
                loginmaps.put(uid, loginmap);
            }
            Map<String, String> onlinemap = onlineMap.get(uid);
            onlinemap.put("uid", uid);
            onlinemap.put("sessionid", sid);
            onlineMap.put(uid, onlinemap);
        }
        return "success";
    }

}
