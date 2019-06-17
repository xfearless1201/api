package com.cn.tianxia.api.game.gameProxy;

import com.cn.tianxia.api.common.v2.OkHttpClient;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.service.v2.UserGameTransferService;
import com.cn.tianxia.api.vo.v2.*;
import com.cn.tianxia.api.web.BaseController;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 *
 * @ClassName AGGameServiceImpl
 * @Description  AG游戏
 * @author Jacky
 * @Date 2019年5月18日 下午4:31:46
 * @version 1.0.0
 */
public class AGGameServiceImpl implements GameInterfaceService {
    private final static Logger logger = LoggerFactory.getLogger(AGGameServiceImpl.class);

    @Autowired
    private UserGameTransferService userGameTransferService;


    private static String api_url;
    private static String api_url_game;
    private static String api_deskey;
    private static String api_md5key;
    private static String api_cagent;
    private static String actype;

    final static okhttp3.OkHttpClient client = OkHttpClient.CLIENT.getClientInstance();

    @PostConstruct
    private void init(){

        //初始化配置信息
        Map<String,String> pmap = userGameTransferService.getPlatformConfig();
    }

    @Override
    public JSONObject transferIn(GameTransferVO gameTransferVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject transferOut(GameTransferVO gameTransferVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject forwardGame(GameForwardVO gameForwardVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject getBalance(GameBalanceVO gameBalanceVO) throws Exception {
        return null;
    }

    @Override
    public JSONObject checkOrCreateAccount(GameCheckOrCreateVO gameCheckOrCreateVO) throws Exception {
        logger.info("");
        return null;
    }

    @Override
    public JSONObject queryTransferOrder(GameQueryOrderVO gameQueryOrderVO) throws Exception {
        return null;
    }



}
