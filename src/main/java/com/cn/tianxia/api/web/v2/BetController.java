package com.cn.tianxia.api.web.v2;

import com.cn.tianxia.api.po.v2.JSONArrayResponse;
import com.cn.tianxia.api.service.v2.GameBetService;
import com.cn.tianxia.api.utils.RedisUtils;
import com.cn.tianxia.api.vo.v2.BetInfoVO;
import com.cn.tianxia.api.web.BaseController;
import net.sf.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Hardy
 * @version 1.0.0
 * @ClassName BetController
 * @Description 投注接口
 * @Date 2019年1月30日 下午8:37:06
 */
@Controller
@RequestMapping("/User")
public class BetController extends BaseController {
    @Autowired
    private GameBetService gameBetService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * @param request
     * @param response
     * @return
     * @Description 获取用户注单信息
     */
    @RequestMapping("/getBetInfo")
    @ResponseBody
    public JSONArray getBetInfo(HttpServletRequest request, HttpServletResponse response, @RequestParam(required = true) String type,
                                @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                                @RequestParam(defaultValue = "1", required = false) Integer pageNo,
                                @RequestParam(required = true) String bdate,
                                @RequestParam(required = true) String edate) {
        logger.info("查询用户注单信息列表开始=================START====================");
        try {
            //获取用户信息
            Map<String, String> data = getUserInfoMap(redisUtils, request);
            if (CollectionUtils.isEmpty(data)) {
                logger.info("获取用户ID失败,登录超时请重新登录");
                return JSONArrayResponse.faild("登录超时,请重新登录");
            }
            String uid = data.get("uid");
            BetInfoVO betInfoVO = new BetInfoVO();
            betInfoVO.setUid(uid);
            betInfoVO.setBdate(bdate);
            betInfoVO.setEdate(edate);
            betInfoVO.setPageNo(pageNo);
            betInfoVO.setPageSize(pageSize);
            betInfoVO.setType(type);
            return gameBetService.getGameBetInfo(betInfoVO, data);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("查询用户注单信息列表异常:{}", e.getMessage());
            return JSONArrayResponse.faild("查询用户注单信息列表异常");
        }
    }
}
