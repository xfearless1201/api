package com.cn.tianxia.api.service.v2;

import com.cn.tianxia.api.project.OrderQuery;
import com.cn.tianxia.api.vo.v2.BetInfoVO;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.Map;

/**
 * @author Hardy
 * @version 1.0.0
 * @ClassName GameBetService
 * @Description 游戏注单接口
 * @Date 2019年1月30日 下午8:55:39
 */
public interface GameBetService {

    public JSONArray getGameBetInfo(BetInfoVO betInfoVO, Map<String, String> data);

    /**
     * @param orderQuery
     * @return
     * @Description 获取BG游戏注单订单
     */
    public JSONObject getBGBetOrder(OrderQuery orderQuery);
}
