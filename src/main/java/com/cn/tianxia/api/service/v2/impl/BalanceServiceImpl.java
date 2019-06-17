package com.cn.tianxia.api.service.v2.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.cn.tianxia.api.common.v2.ResultResponse;
import com.cn.tianxia.api.domain.txdata.v2.NewUserDao;
import com.cn.tianxia.api.domain.txdata.v2.UserGamestatusDao;
import com.cn.tianxia.api.game.GameInterfaceService;
import com.cn.tianxia.api.game.GameProxyFactory;
import com.cn.tianxia.api.po.v2.BalancePO;
import com.cn.tianxia.api.project.v2.UserGamestatusEntity;
import com.cn.tianxia.api.service.v2.BalanceService;
import com.cn.tianxia.api.vo.v2.GameBalanceVO;

import net.sf.json.JSONObject;

/**
 * 
 * @ClassName BalanceServiceImpl
 * @Description 余额接口实现类
 * @author Hardy
 * @Date 2019年6月2日 下午10:35:49
 * @version 1.0.0
 */
@Service
public class BalanceServiceImpl implements BalanceService {
    
    //日志
    private static final Logger logger = LoggerFactory.getLogger(BalanceServiceImpl.class);
    
    private static ExecutorService cachedThreadExecutor = Executors.newFixedThreadPool(10);
    //游戏前缀
    private static final String GAME_TYPE_PREFIX="is_";
    
    @Autowired
    private UserGamestatusDao userGamestatusDao;
    
    @Autowired
    private NewUserDao newUserDao;
    
    @Override
    public ResultResponse getUserAllBalance(GameBalanceVO gameBalanceVO,Set<String> types) throws Exception{
        List<BalancePO> response = new ArrayList<BalancePO>();
        //初始化用户钱包余额
        initBalance(types, response);
        try {
            if(types.contains("WALLET")){
                JSONObject walletdata = new JSONObject();
                //查询用户钱包余额
                double wallet = newUserDao.queryUserBalance(Integer.parseInt(gameBalanceVO.getUid()));
                Iterator<BalancePO> iterator = response.iterator();
                while (iterator.hasNext()) {
                    BalancePO balancePO = iterator.next();
                    if(balancePO.getType().equalsIgnoreCase("WALLET")){
                        balancePO.setBalance(new DecimalFormat("0.00").format(wallet));
                    }
                    
                }
            }
            Set<String> gametypes = new HashSet<>();
            types.stream().forEach(type->{
                if(type.equalsIgnoreCase("sb")){
                    //申博
                    gametypes.add(GAME_TYPE_PREFIX+"shenbo");
                }else{
                    gametypes.add(GAME_TYPE_PREFIX+type.toLowerCase());
                }
            });
            //查询用户
            List<UserGamestatusEntity> userGamestatus = userGamestatusDao.findAllByGameTypes(gameBalanceVO.getUid(), gametypes);
            if(CollectionUtils.isEmpty(userGamestatus)){
                logger.info("用户【"+gameBalanceVO.getGamename()+"】查询用户游戏状态列表失败,请求类型集合:{}",types.toString());
                return ResultResponse.success("查询成功", response);
            }
            //比较,组装会员存在的游戏类型
            Set<String> sets = new HashSet<>();
            for (UserGamestatusEntity userGamestatusEntity : userGamestatus) {
                for (String gametype : gametypes) {
                    if(userGamestatusEntity.getGametype().equalsIgnoreCase(gametype)){
                        if(gametype.equalsIgnoreCase(GAME_TYPE_PREFIX+"shenbo")){
                            sets.add("SB");
                        }else{
                            sets.add(gametype.replace(GAME_TYPE_PREFIX, "").toUpperCase());
                        }
                    }
                }
            }
            List<Future<JSONObject>> futures = new ArrayList<Future<JSONObject>>();  
            //查询所有的游戏余额
            for (String type : sets) {
                FutureTask<JSONObject> dbtask = new FutureTask<>(new Callable<JSONObject>() {
                    @Override
                    public JSONObject call() throws Exception {
                        try {
                            GameInterfaceService service = GameProxyFactory.productGameService(type);
                            return service.getBalance(gameBalanceVO);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JSONObject errordata = new JSONObject();
                            errordata.put("type", type);
                            errordata.put("balance", 0.00);
                            return errordata;
                        }
                    }
                });
                cachedThreadExecutor.submit(dbtask);
                futures.add(dbtask);
            }
            
            for (BalancePO balance : response) {
                for (Future<JSONObject> future : futures) {
                    if(balance.getType().equalsIgnoreCase(future.get().getString("type"))){
                        balance.setBalance(future.get().getString("balance"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(cachedThreadExecutor.isTerminated()){
                cachedThreadExecutor.shutdown();
            }
        }
        return ResultResponse.success("查询成功", response);
    }
    
    
    private void initBalance(Set<String> types,List<BalancePO> response){
        types.stream().forEach(item->{
            BalancePO balances = new BalancePO();
            balances.setBalance(new DecimalFormat("0.00").format(0.00));
            balances.setType(item);
            response.add(balances);
        });
    }

}
