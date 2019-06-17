package com.cn.tianxia.api.service.v2.impl;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.cn.tianxia.api.domain.ftpdata.GameBetInfoDao;
import com.cn.tianxia.api.domain.txdata.v2.PlatformConfigDao;
import com.cn.tianxia.api.game.impl.BGGameServiceImpl;
import com.cn.tianxia.api.po.BaseResponse;
import com.cn.tianxia.api.po.v2.JSONArrayResponse;
import com.cn.tianxia.api.project.OrderQuery;
import com.cn.tianxia.api.project.v2.GameBetInfoEntity;
import com.cn.tianxia.api.project.v2.PlatformConfigEntity;
import com.cn.tianxia.api.service.v2.GameBetService;
import com.cn.tianxia.api.utils.PlatFromConfig;
import com.cn.tianxia.api.vo.v2.BetInfoVO;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author Hardy
 * @version 1.0.0
 * @ClassName GameBetServiceImpl
 * @Description 游戏注单实现类
 * @Date 2019年1月30日 下午8:56:09
 */
@Service
public class GameBetServiceImpl implements GameBetService {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(GameBetServiceImpl.class);

    @Autowired
    private GameBetInfoDao gameBetInfoDao;

    @Autowired
    private PlatformConfigDao platformConfigDao;

    /**
     * 查询用户注单信息
     */
    public JSONArray getGameBetInfo(BetInfoVO betInfoVO, Map<String, String> dataMap) {
        logger.info("调用查询用户游戏注单列表业务开始==================START================");
        JSONArray data = new JSONArray();
        try {
            //获取用户信息
            String agUsername = dataMap.get("ag_username");//ag游戏账号
            String cagent = dataMap.get("cagent");//平台编码
            String type = betInfoVO.getType().toUpperCase();//游戏编码

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //日期格式化
            SimpleDateFormat ssdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String bdate = betInfoVO.getBdate();//起始时间
            String edate = betInfoVO.getEdate();//结束时间
            if (StringUtils.isBlank(bdate)) {
                bdate = ssdf.format(new Date()) + " 00:00:00";
            }

            if (StringUtils.isBlank(edate)) {
                edate = ssdf.format(new Date()) + " 23:59:59";
            }
            Date btime = sdf.parse(bdate);
            Date etime = sdf.parse(edate);

            int pageNo = betInfoVO.getPageNo();
            int pageSize = betInfoVO.getPageSize();

            if ("HG".equals(type)) {
                agUsername = dataMap.get("hg_username");
            }

            if (StringUtils.isBlank(agUsername)) {
                logger.info("查询用户游戏登录账号为空");
                return JSONArrayResponse.faild("查询用户游戏登录账号为空");
            }

            //查询游戏注单列表
            List<GameBetInfoEntity> gameBetInfos =
                    gameBetInfoDao.findAllByPage(agUsername, cagent, type, btime, etime, pageNo, pageSize);
            //统计注单
            Double subPayoutSum = 0d;//统计注单派彩总金额
            Double subBetamountSum = 0d;//统计投注总金额
            Double subNetAmountSum = 0d;//统计玩家输赢总金额
            Double subValidBetAmountSum = 0d;//统计玩家有效总投注
            JSONArray betinfosArray = new JSONArray();
            DecimalFormat df = new DecimalFormat("0.00");
            if (!CollectionUtils.isEmpty(gameBetInfos)) {
                subPayoutSum = Double.parseDouble(df.format(gameBetInfos.stream().mapToDouble(GameBetInfoEntity::getPayout).sum()));
                subBetamountSum = Double.parseDouble(df.format(gameBetInfos.stream().mapToDouble(GameBetInfoEntity::getBetAmount).sum()));
                subNetAmountSum = Double.parseDouble(df.format(gameBetInfos.stream().mapToDouble(GameBetInfoEntity::getNetAmount).sum()));
                subValidBetAmountSum = Double.parseDouble(df.format(gameBetInfos.stream().mapToDouble(GameBetInfoEntity::getValidBetAmount).sum()));
                for (GameBetInfoEntity gameBetInfo : gameBetInfos) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", gameBetInfo.getId());
                    jsonObject.put("bettime", sdf.parse(gameBetInfo.getBettime()).getTime());
                    jsonObject.put("type", gameBetInfo.getType());
                    Double betAmount = Double.parseDouble(df.format(gameBetInfo.getBetAmount()));
                    Double validBetAmount = Double.parseDouble(df.format(gameBetInfo.getValidBetAmount()));
                    Double Payout = Double.parseDouble(df.format(gameBetInfo.getPayout()));
                    Double netAmount = Double.parseDouble(df.format(gameBetInfo.getNetAmount()));
                    jsonObject.put("betAmount", betAmount);
                    jsonObject.put("validBetAmount", validBetAmount);
                    jsonObject.put("Payout", Payout);
                    jsonObject.put("netAmount", netAmount);
                    betinfosArray.add(jsonObject);
                }
            }

            //查询游戏注单总数
            Map<String, Object> totalCounts = gameBetInfoDao.selectBetCount(agUsername, cagent, type, btime, etime);
            if (CollectionUtils.isEmpty(totalCounts)) {
                totalCounts = new HashMap<>();
                totalCounts.put("cnt", 0);
                totalCounts.put("betamountTotal", 0.00);
                totalCounts.put("netAmountTotal", 0.00);
                totalCounts.put("payoutTotal", 0.00);
                totalCounts.put("validBetAmountTotal", 0.00);
            }
            
            logger.info("打印统计用户:["+betInfoVO.getUid()+"]游戏注单总数结果：{}",totalCounts.toString());
            
            JSONObject pagesJson = new JSONObject();
            pagesJson.put("cnt", totalCounts.get("cnt"));
            data.add(pagesJson);
            JSONObject sumJson = new JSONObject();
            DecimalFormat dcf = new DecimalFormat("0.00");
            sumJson.put("payoutSum", dcf.format(subPayoutSum));
            sumJson.put("betamountSum", dcf.format(subBetamountSum));
            sumJson.put("netAmountSum", dcf.format(subNetAmountSum));
            sumJson.put("validBetAmountSum", dcf.format(subValidBetAmountSum));
            sumJson.put("payoutTotal", dcf.format(totalCounts.get("payoutTotal")));
            sumJson.put("betamountTotal", dcf.format(totalCounts.get("betamountTotal")));
            sumJson.put("netAmountTotal", dcf.format(totalCounts.get("netAmountTotal")));
            sumJson.put("validBetAmountTotal", dcf.format(totalCounts.get("validBetAmountTotal")));
            data.add(sumJson);
            data.addAll(betinfosArray);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("调用查询用户游戏注单列表业务异常:{}", e.getMessage());
        }
        return data;
    }

    /**
     * 获取BG游戏注单订单
     */
    @Override
    public JSONObject getBGBetOrder(OrderQuery orderQuery) {
        logger.info("调用获取游戏注单订单业务开始================START====================");
        try {

            //获取所有游戏配置信息
            List<PlatformConfigEntity> platformConfigs = platformConfigDao.findAll();
            if (!CollectionUtils.isEmpty(platformConfigs)) {
                logger.info("BG游戏维护中,获取BG游戏配置信息为空");
                return BaseResponse.error("process", "查询游戏配置信息失败");
            }

            //转成map
            Map<String, String> data = platformConfigs.stream().collect(Collectors.toMap(PlatformConfigEntity::getPlatformKey, PlatformConfigEntity::getPlatformConfig));
            if (CollectionUtils.isEmpty(data)) {
                logger.info("BG游戏维护中,获取BG游戏配置信息为空");
                return BaseResponse.error("process", "查询游戏配置信息失败");
            }

            PlatFromConfig pf = new PlatFromConfig();
            pf.InitData(data, "BG");
            if ("0".equals(pf.getPlatform_status())) {
                return BaseResponse.error("0", "process");
            }
            BGGameServiceImpl gameService = new BGGameServiceImpl(JSONObject.fromObject(pf.getPlatform_config()));
            JSONObject jsonObject = gameService.orderAgentQuery(orderQuery);
            return jsonObject;
        } catch (Exception e) {
            logger.info("调用获取游戏注单订单业务异常");
            e.printStackTrace();
            return BaseResponse.error("0", "error");
        }
    }
}
