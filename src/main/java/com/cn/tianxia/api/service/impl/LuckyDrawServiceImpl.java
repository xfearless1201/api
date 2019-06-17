package com.cn.tianxia.api.service.impl;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.cn.tianxia.api.base.annotation.DataSource;
import com.cn.tianxia.api.base.datashource.Database;
import com.cn.tianxia.api.common.v2.BaseResponse;
import com.cn.tianxia.api.common.v2.DatePatternConstant;
import com.cn.tianxia.api.common.v2.DatePatternUtils;
import com.cn.tianxia.api.domain.ftpdata.GameBetInfoDao;
import com.cn.tianxia.api.domain.txdata.LuckyDrawDao;
import com.cn.tianxia.api.domain.txdata.v2.CagentDao;
import com.cn.tianxia.api.domain.txdata.v2.CagentLuckyDrawDao;
import com.cn.tianxia.api.domain.txdata.v2.CagentLuckyDrawDetailDao;
import com.cn.tianxia.api.domain.txdata.v2.NewUserDao;
import com.cn.tianxia.api.domain.txdata.v2.RefererUrlDao;
import com.cn.tianxia.api.domain.txdata.v2.UserLuckrdrawLogDao;
import com.cn.tianxia.api.domain.txdata.v2.UserTreasureDao;
import com.cn.tianxia.api.project.v2.CagentEntity;
import com.cn.tianxia.api.project.v2.CagentLuckyDrawDetailEntity;
import com.cn.tianxia.api.project.v2.CagentLuckyDrawEntity;
import com.cn.tianxia.api.project.v2.UserEntity;
import com.cn.tianxia.api.project.v2.UserLuckrdrawLogEntity;
import com.cn.tianxia.api.service.LuckyDrawService;
import com.cn.tianxia.api.utils.v2.DateUtils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;

/**
 * 功能概要：UserService实现类
 * 
 */
@Service
public class LuckyDrawServiceImpl implements LuckyDrawService {
    
    //日志
    private static final Logger logger = LoggerFactory.getLogger(LuckyDrawServiceImpl.class);
	
    @Resource
	private LuckyDrawDao luckyDrawDao;
	@Autowired
	private NewUserDao newUserDao;
	@Autowired
	private CagentDao cagentDao;
	@Autowired
	private CagentLuckyDrawDao cagentLuckyDrawDao;
	@Autowired
	private CagentLuckyDrawDetailDao cagentLuckyDrawDetailDao;
	@Autowired
	private UserLuckrdrawLogDao userLuckrdrawLogDao;
	@Autowired
	private UserTreasureDao userTreasureDao;
	@Autowired
    private GameBetInfoDao gameBetInfoDao;
	
	@Autowired
	private RefererUrlDao refererUrlDao;

	@Override
	public List<Map<String, Object>> selectLuckyDrawStatus(String domain) { 
		return luckyDrawDao.selectLuckyDrawStatus(domain);
	}
	
	@DataSource(Database.FTPDATA_XMLDB_MASTER)
	private void game(String username){
        gameBetInfoDao.findAllByPage(username, "BL1", "AG", null, null, 0, 10);
    }
	
	
	/**
	 * 
	 * @Description 红包抽奖
	 * @param username
	 * @param refurl
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Override
    public JSONObject luckyDraw(String username,String refurl) throws Exception{
	    try {
            //获取用户基本信息
	        JSONObject data = getUserBaseInfo(username, refurl);
	        if(data.getString("status").equalsIgnoreCase("faild")){
	            return data;
	        }
	        //活动ID
	        Integer uid = data.getInteger("uid");//用户ID
	        //平台ID
	        Integer cid = data.getInteger("cid");
	        //获取平台红包活动配置信息
	        CagentLuckyDrawEntity luckyDrawEntity = data.getObject("luckyDrawEntity", CagentLuckyDrawEntity.class);
	        //获取活动抽奖类型,抽奖模式 1 充值金额达标 2有效注单金额达标
            String typesof = luckyDrawEntity.getTypesof();
	        //获取金额计算方式 0:当日金额 1:昨日金额
	        String type = luckyDrawEntity.getType();
	        Date begin = DateUtils.getDayBegin();
	        Date end = DateUtils.getDayEnd();
	        if(type.equals("1")){ //昨日的开始和结束时间
                begin = DateUtils.getDayBeginOfYesterday();
                end = DateUtils.getDayEndOfYesterDay();
            }
	        //获取平台活动详情列表
	        List<CagentLuckyDrawDetailEntity> details = data.getObject("details",List.class);
	        
	        //平台剩余额度
            double remainvalue = data.getDouble("remainvalue");
	        //用户已用额度
//	        double usedLimitAmount = data.getDouble("usedLimitAmount");
	        //判断用户充值金额或者有效注单金额是否达标
	        JSONObject jsonObject = new JSONObject();
	        if(typesof.equals("1")){
	            jsonObject = luckyDrawByTopUp(username,uid, begin, end, details);
	        }else{
	            jsonObject = luckyDrawByValidBet(username,uid, begin, end, details);
	        }
	        
	        if(jsonObject.getString("status").equalsIgnoreCase("faild")){
	            return jsonObject;
	        }
	        //用户存款金额或注单金额匹配的抽奖次数
	        int luckyDraws = jsonObject.getInteger("luckyDraws");
	        //已使用的存款额度或注单总额，用于更新用户红包记录表
	        double usedBet = jsonObject.getDouble("usedBet");
	        //进行抽奖活动
	        return startLuckyDraw(username,uid,luckyDraws,usedBet,remainvalue,cid,refurl,luckyDrawEntity);
	    } catch (Exception e) {
            e.printStackTrace();
            logger.info("用户:【"+username+"LuckyDraw】调用红包抽奖业务异常:{}",e.getMessage());
            return BaseResponse.faild("faild", "抽奖异常,请联系客服");
        }
	}
	
	
	/**
	 * 
	 * @Description 开始抽奖活动
	 * @param username 用户名
	 * @param uid      用户ID
	 * @param luckyDraws 抽奖次数
	 * @param usedBet 使用金额
	 * @param remainvalue 平台剩余额度
	 * @param usedLimitAmount 用户已用额度
	 * @param cid  平台ID
	 * @param refurl   请求域名
	 * @param luckyDrawEntity 平台红包配置信息
	 * @return
	 */
	@Transactional(propagation=Propagation.REQUIRED,rollbackFor = Exception.class)
	public synchronized JSONObject startLuckyDraw(String username,int uid,int luckyDraws,double usedBet,double remainvalue,
	                                        int cid,String refurl,CagentLuckyDrawEntity luckyDrawEntity){
	    try {
	        String bTime = DatePatternUtils.dateToStr(luckyDrawEntity.getBegintime(),DatePatternConstant.NORM_TIME_PATTERN);
            String eTime = DatePatternUtils.dateToStr(luckyDrawEntity.getEndtime(),DatePatternConstant.NORM_TIME_PATTERN);
            Date beginTime = DatePatternUtils.strToDate(DatePatternUtils.dateToStr(new Date(),DatePatternConstant.NORM_DATE_PATTERN) + " " +  bTime, DatePatternConstant.NORM_DATETIME_PATTERN);
            Date endTime = DatePatternUtils.strToDate(DatePatternUtils.dateToStr(new Date(),DatePatternConstant.NORM_DATE_PATTERN) + " " +  eTime, DatePatternConstant.NORM_DATETIME_PATTERN);
            Date now = new Date();
            //未到抽奖时间
            if (now.before(beginTime)) {
                return responseResult("waiting", now, beginTime, endTime, "未到活动时间");
            }
            //超过活动时间
            if (now.after(endTime)) {
                return responseResult("end", now, beginTime, endTime, "今日活动已结束");
            }
            //红包派发金额
            double result = 0.00;
            float min = luckyDrawEntity.getMinamount();
            float max = luckyDrawEntity.getMaxamount();
            if (min >= max) {
                luckyDrawDao.updateStatusByAmount(luckyDrawEntity.getId(), "1");
                return BaseResponse.faild("faild", "活动已结束");
            }
            result = min + Math.random() * (max - min);
            DecimalFormat dft = new DecimalFormat("0.00");
            result = Double.parseDouble(dft.format(result));
            //判断平台额度是否足够
            if(remainvalue < result){
                return BaseResponse.faild("faild", "平台额度已不足");
            }
            
            //查询会员已用抽奖次数
            List<UserLuckrdrawLogEntity> usedLogs = userLuckrdrawLogDao.selectByUidAndLid(uid,luckyDrawEntity.getId(),beginTime,now,luckyDrawEntity.getTypesof());
//            Optional<UserLuckrdrawLogEntity> maxRecord = usedLogs.stream().max(Comparator.comparingDouble(UserLuckrdrawLogEntity::getUsedBet));
//            if (maxRecord.isPresent()) {
//                usedBet = usedBet + usedLimitAmount;
//            }
            //用户总的抽奖次数
            int usedTime = usedLogs.size();
            logger.info("用户:【"+ username +"LuckyDraw】,该活动的可用抽奖次数:【"+ luckyDraws + "】,已用抽奖次数:【" + usedTime + "】");
            //判断用户抽奖次数
            if(usedTime >= luckyDraws){
                return BaseResponse.faild("faild", "已无抽奖次数");
            }
            //奖金池剩余金额
            double luckyBalance = luckyDrawDao.selectLuckyDrawsBalance(luckyDrawEntity.getId());
            //先判断奖金池的金额是否符合抽奖
            if(luckyBalance == 0 ||luckyBalance < min){
                luckyDrawDao.updateStatusByAmount(luckyDrawEntity.getId(), "1");
                return BaseResponse.faild("faild", "活动已结束");
            }else {
                if(result > luckyBalance){
                    result = luckyBalance;
                }
            }
            //更新抽奖已用金额
            luckyDrawDao.updateLuckydraw(luckyDrawEntity.getId(),result);
            logger.info("用户【"+username+"LuckyDraw】添加抽奖日志：uid:{"+uid+"},lid:{"+luckyDrawEntity.getId()+"},cid:{"+cid+"},amount:{"+result+"}");
            //查询用户今日所有抽奖次数
            int userHasDrawTimes = 0;
            int userTotalHasDrawTimes = 0;
            //查询用户最近的一条抽奖记录
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String luckyStartTime = sdf.format(new Date())+" 00:00:00";
            String luckyEndTime = sdf.format(new Date())+" 23:59:59";
            UserLuckrdrawLogEntity recentRecord = userLuckrdrawLogDao.selectRecentRecord(uid,luckyStartTime,luckyEndTime);
            if (recentRecord != null) {
                userHasDrawTimes = recentRecord.getTodaytimes();
                userTotalHasDrawTimes = recentRecord.getTotaltimes();
            }
            
            UserLuckrdrawLogEntity newLuckDarwLog = new UserLuckrdrawLogEntity();
            //添加抽奖日志
            newLuckDarwLog.setAddtime(now);
            newLuckDarwLog.setAmount(Double.valueOf(result).floatValue());
            newLuckDarwLog.setUid(uid);
            newLuckDarwLog.setLid(luckyDrawEntity.getId());
            newLuckDarwLog.setCid(cid);
            newLuckDarwLog.setOrderid("HB" + System.currentTimeMillis());
            newLuckDarwLog.setIp(refurl);
            newLuckDarwLog.setTypesof(luckyDrawEntity.getTypesof());
            newLuckDarwLog.setUsedBet(usedBet);
            newLuckDarwLog.setTodaytimes(userHasDrawTimes + 1);
            newLuckDarwLog.setTotaltimes(userTotalHasDrawTimes + 1);
            userLuckrdrawLogDao.insertSelective(newLuckDarwLog);
            JSONObject data = new JSONObject();
            data.put("status", "success");
            data.put("result", result);
            data.put("msg", "抽奖成功");
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("用户:【"+username+"LuckyDraw】调用红包抽奖业务异常:{}",e.getMessage());
            return BaseResponse.faild("faild", "抽奖异常,请联系客服");
        }
	}
	
	private JSONObject luckyDrawByTopUp(String username,Integer uid,Date begin,Date end,List<CagentLuckyDrawDetailEntity> details){
	    //获取用户充值或加款的金额
        Double userDidAmount = userTreasureDao.findAllAmountByTime(uid,DatePatternUtils.dateToStr(begin,DatePatternConstant.NORM_DATETIME_PATTERN),DatePatternUtils.dateToStr(end,DatePatternConstant.NORM_DATETIME_PATTERN));
        /** 2019-05-13 新需求,按照用户的总的抽奖次数来决定：
         * 逻辑1：若用户A在活动一进行时，没有进行抽奖，则他在活动二中的总存款为26000，即：他可以在活动二中抽奖20次。
         * 逻辑2：若用户A在活动一时，将10次抽奖机会全部抽完，则在活动二时，用户累计总存款依然为26000，判断该用户在活动一已经抽奖10次，则在活动二中，用户的抽奖次数为20 - 10 = 10，即减去上一次活动已抽奖的次数。
         * 逻辑3：若用户A在活动一时，只抽取了5次，剩余5次，则在活动二时，用户累计总存款依然为26000，判断该用户在活动一已经抽奖5次，则在活动二中，用户的抽奖次数为20 - 5 = 15，即减去上一次活动已抽奖的次数。
         */
//        double leftAmount = userDidAmount - usedLimitAmount;
        logger.info("查询用户:【"+username+"】,充值加款总金额:【"+userDidAmount+"】,抽奖起始时间:【"+DateUtil.format(begin, DatePattern.NORM_DATETIME_PATTERN)+"】,抽奖结束时间:【"+DateUtil.format(end, DatePattern.NORM_DATETIME_PATTERN)+"】");
        //获取剩余额度匹配的活动次数
        Optional<CagentLuckyDrawDetailEntity> maxTime = details.stream().filter(n -> n.getBalance() <= userDidAmount).max(Comparator.comparingDouble(CagentLuckyDrawDetailEntity::getBalance));
        if (!maxTime.isPresent()) {
            logger.info("用户:【"+username+"LuckyDraw】,充值金额未达标,用户充值金额为:【"+userDidAmount+"】,平台抽奖满足额度:{}",details.toString());
            return BaseResponse.faild("faild", "充值金额未达标");
        }
        logger.info("查询用户:【"+username+"】,充值加款总金额:【"+userDidAmount+"】,平台红包抽奖充值限额:【"+maxTime.get().getBalance()+"】抽奖起始时间:【"+DateUtil.format(begin, DatePattern.NORM_DATETIME_PATTERN)+"】,抽奖结束时间:【"+DateUtil.format(end, DatePattern.NORM_DATETIME_PATTERN)+"】");
        //抽奖次数
        int luckyDraws = maxTime.get().getTimes();
        //使用金额
        double usedBet = Double.valueOf(maxTime.get().getBalance());
	    return responseResult("success",luckyDraws,usedBet,"充值金额达标");
	}
	
	private JSONObject luckyDrawByValidBet(String username,Integer uid,Date begin,Date end,List<CagentLuckyDrawDetailEntity> details){
	    logger.info("查询用户注单金额达标请求参数报文:【"+username+"】,开始时间:【"+begin+"】,结束时间:【"+end+"】");
	    Double vilidBetAmount = gameBetInfoDao.selectUserValidBetAmuontList(uid, begin, end);
	    /** 2019-05-13 新需求,按照用户的总的抽奖次数来决定：
	     * 逻辑1：若用户A在活动一进行时，没有进行抽奖，则他在活动二中的总存款为26000，即：他可以在活动二中抽奖20次。
	     * 逻辑2：若用户A在活动一时，将10次抽奖机会全部抽完，则在活动二时，用户累计总存款依然为26000，判断该用户在活动一已经抽奖10次，则在活动二中，用户的抽奖次数为20 - 10 = 10，即减去上一次活动已抽奖的次数。
	     * 逻辑3：若用户A在活动一时，只抽取了5次，剩余5次，则在活动二时，用户累计总存款依然为26000，判断该用户在活动一已经抽奖5次，则在活动二中，用户的抽奖次数为20 - 5 = 15，即减去上一次活动已抽奖的次数。
	     */
//        double leftAmount = vilidBetAmount - usedLimitAmount;
	    //获取剩余额度匹配的活动次数
        Optional<CagentLuckyDrawDetailEntity> maxTime = details.stream().filter(n -> n.getValidbetamount() <= vilidBetAmount).max(Comparator.comparingDouble(CagentLuckyDrawDetailEntity::getValidbetamount));
        logger.info("查询用户:【"+username+"】,注单总金额:【"+vilidBetAmount+"】,抽奖起始时间:【"+DateUtil.format(begin, DatePattern.NORM_DATETIME_PATTERN)+"】,抽奖结束时间:【"+DateUtil.format(end, DatePattern.NORM_DATETIME_PATTERN)+"】");
        if (!maxTime.isPresent()) {
            logger.info("用户:【"+username+"LuckyDraw】,注单金额未达标");
            return BaseResponse.faild("faild", "有效注单金额未达标");
        }
        logger.info("查询用户:【"+username+"】,注单总金额:【"+vilidBetAmount+"】,平台红包抽奖注单限额:【"+maxTime.get().getValidbetamount()+"】抽奖起始时间:【"+DateUtil.format(begin, DatePattern.NORM_DATETIME_PATTERN)+"】,抽奖结束时间:【"+DateUtil.format(end, DatePattern.NORM_DATETIME_PATTERN)+"】");
        //抽奖次数
        int luckyDraws = maxTime.get().getTimes();
        //使用金额
        double usedBet = Double.valueOf(maxTime.get().getValidbetamount());
        return responseResult("success",luckyDraws,usedBet,"注单金额达标");
	}
	
	/**
	 * 
	 * @Description 获取用户基本信息
	 * @param username
	 * @param referUrl
	 * @return
	 */
	private JSONObject getUserBaseInfo(String username,String referUrl){
	    try {
	        //通过用户名查询用户信息
            UserEntity user = newUserDao.selectByUsername(username);
            if(user == null){
                return BaseResponse.faild("faild", username+"用户账号不存在");
            }
            //判断用户请求域名是否合法
            int count = refererUrlDao.checkReferUrlByCagent(referUrl,user.getCagent());
            if(count == 0){
                logger.warn("用户:【"+username+"LuckyDraw】,请求来源域名不正确:{}",referUrl);
                return BaseResponse.faild("faild", "来源域名不正确");
            }
            //通过平台编码查询平台ID
            CagentEntity cagentEntity = cagentDao.selectByCagent(user.getCagent());
            if(cagentEntity == null){
                logger.warn("用户:【"+username+"LuckyDraw】,查询所属平台失败,平台编码:{}",user.getCagent());
                return BaseResponse.faild("faild", "非法平台");
            }
            
            //通过平台ID查询平台活动配置信息
            CagentLuckyDrawEntity luckyDrawEntity = cagentLuckyDrawDao.selectByCid(cagentEntity.getId());
            if(luckyDrawEntity == null){
                return BaseResponse.faild("faild", "平台暂无红包抽奖活动");
            }
            //获取平台红包活动抽奖模式 1 充值金额达标 2有效注单金额达标
//            String typesof = luckyDrawEntity.getTypesof();
            //通过活动ID查询活动详情
            List<CagentLuckyDrawDetailEntity> details = cagentLuckyDrawDetailDao.selectByLid(luckyDrawEntity.getId());
            if(CollectionUtils.isEmpty(details)){
                logger.warn("用户:【"+username+"LuckyDraw】,通过网站活动域名:【"+referUrl+"】,查询平台网站活动失败");
                return BaseResponse.faild("faild", "平台暂无红包抽奖活动");
            }
            
//            String bTime = DatePatternUtils.dateToStr(luckyDrawEntity.getBegintime(),DatePatternConstant.NORM_TIME_PATTERN);
//            Date beginTime = DatePatternUtils.strToDate(DatePatternUtils.dateToStr(new Date(),DatePatternConstant.NORM_DATE_PATTERN) + " " +  bTime, DatePatternConstant.NORM_DATETIME_PATTERN);
//            //查询用户上一次活动抽奖记录,从今天开始到本次活动开始时间
//            List<UserLuckrdrawLogEntity> userLuckyDrawLogs = userLuckrdrawLogDao.selectByUidAndLid(user.getUid(),luckyDrawEntity.getId(),DateUtils.getDayBegin(),beginTime,typesof);
//            Double usedLimitAmount = 0D;  //用户已用额度
//            if (userLuckyDrawLogs != null && userLuckyDrawLogs.size() > 0) {
//                Optional<UserLuckrdrawLogEntity> maxRecord = userLuckyDrawLogs.stream().max(Comparator.comparingDouble(UserLuckrdrawLogEntity::getUsedBet));
//                usedLimitAmount = maxRecord.get().getUsedBet();
//            }
            
            //查询平台剩余额度
            Map<String, Object> cagentStoredvalue = luckyDrawDao.selectByCidCagentStoredvalue(cagentEntity.getId());
            double remainvalue = 0.00;
            if(!CollectionUtils.isEmpty(cagentStoredvalue)){
                if(cagentStoredvalue.containsKey("remainvalue") && StringUtils.isNoneEmpty(cagentStoredvalue.get("remainvalue").toString())){
                    remainvalue = Double.parseDouble(cagentStoredvalue.get("remainvalue").toString());
                }
            }
            //设置用户基本信息
            JSONObject data = new JSONObject();
            data.put("uid", user.getUid());//用户ID
            data.put("luckyDrawEntity", luckyDrawEntity);//平台活动配置信息
            data.put("details", details);//活动详情列表
//            data.put("usedLimitAmount", usedLimitAmount);
            data.put("remainvalue", remainvalue);//平台剩余额度
            data.put("cid", cagentEntity.getId());
            data.put("status", "success");
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("用户【"+username+"LuckyDraw】,参与红包抽奖活动异常:{}",e.getMessage());
            return BaseResponse.faild("faild", "红包活动异常,联系客服");
        }
	}
	
	
	
	
	/**
	 * 抽取红包
	 */
//	public synchronized JSONObject luckyDrawTest(String userName,String refurl)throws Exception{
//	    logger.info("用户:【"+userName+"LuckyDraw】调用红包抽奖业务开始==============START=====================");
//	    try {
//	        JSONObject jo = new JSONObject();
//	        // 获取来源域名
//	        if (StringUtils.isBlank(userName)) {
//	            logger.info("用户:【"+userName+"LuckyDraw】用户名为空");
//	            jo.put("status", "faild");
//	            jo.put("msg", "请传入用户名");
//	            return jo;
//	        }
//
//			logger.info("用户:【"+userName+"LuckyDraw】,通过用户名查询用户信息-----------开始------------");
//
//			UserEntity user = newUserDao.selectByUsername(userName);
//			
//			if (null == user) {
//				logger.info("用户:【"+userName+"LuckyDraw】,通过用户名查询用户信息,查询失败");
//				jo.put("status", "faild");
//				jo.put("msg", "未查询到会员信息");
//				return jo;
//			}
//
//			logger.info("用户:【"+userName+"LuckyDraw】,通过用户名查询用户信息:{}",user.toString());
//
//			logger.info("用户:【"+userName+"LuckyDraw】,通过网站活动域名:【"+refurl+"】,查询平台网站活动-------------开始---------------");
//			// 通过域名查询代理商信息
//			CagentEntity cagentEntity = cagentDao.selectByRefererUrl(refurl);
//			if (cagentEntity == null) {
//				jo.put("status", "faild");
//				jo.put("msg", "来源域名不正确");
//				return jo;
//			}
//			//查询平台活动
//			CagentLuckyDrawEntity luckyDrawEntity = cagentLuckyDrawDao.selectByCid(cagentEntity.getId());
//			if (luckyDrawEntity == null) {
//				logger.info("用户:【"+userName+"LuckyDraw】,通过网站活动域名:【"+refurl+"】,查询平台网站活动失败");
//				jo.put("status", "faild");
//				jo.put("msg", "暂无活动");
//				return jo;
//			}
//			//活动详情
//			List<CagentLuckyDrawDetailEntity> details = cagentLuckyDrawDetailDao.selectByLid(luckyDrawEntity.getId());
//			if (details == null || details.size() <= 0) {
//				logger.info("用户:【"+userName+"LuckyDraw】,通过网站活动域名:【"+refurl+"】,查询平台网站活动失败");
//				jo.put("status", "faild");
//				jo.put("msg", "活动异常");
//				return jo;
//			}
//			logger.info("用户:【"+userName+"LuckyDraw】,查询到的活动详情:{}",Arrays.toString(details.toArray()));
//
//			String bTime = DatePatternUtils.dateToStr(luckyDrawEntity.getBegintime(),DatePatternConstant.NORM_TIME_PATTERN);
//			String eTime = DatePatternUtils.dateToStr(luckyDrawEntity.getEndtime(),DatePatternConstant.NORM_TIME_PATTERN);
//
//			Date beginTime = DatePatternUtils.strToDate(DatePatternUtils.dateToStr(new Date(),DatePatternConstant.NORM_DATE_PATTERN) + " " +  bTime, DatePatternConstant.NORM_DATETIME_PATTERN);
//			Date endTime = DatePatternUtils.strToDate(DatePatternUtils.dateToStr(new Date(),DatePatternConstant.NORM_DATE_PATTERN) + " " +  eTime, DatePatternConstant.NORM_DATETIME_PATTERN);
//
//			Date now = new Date();
//			//未到抽奖时间
//			if (now.before(beginTime)) {
//				logger.info("用户:【"+userName+"LuckyDraw】,在此时间段,起始时间:【"+beginTime+"】,结束时间:【"+endTime+"】查询抽奖失败");
//				jo.put("status", "waiting");
//				jo.put("now", now);
//				jo.put("begintime", beginTime);
//				jo.put("endtime", endTime);
//				jo.put("diff", - now.getTime() - beginTime.getTime() / 1000);
//				jo.put("msg", "未到活动时间");
//				return jo;
//			}
//			//超过活动时间
//			if (now.after(endTime)) {
//				logger.info("用户:【"+userName+"LuckyDraw】,在此时间段,起始时间:【"+beginTime+"】,结束时间:【"+beginTime+"】查询抽奖已结束");
//				jo.put("status", "end");
//				jo.put("now", now);
//				jo.put("begintime", beginTime);
//				jo.put("endtime", endTime);
//				jo.put("msg", "今日活动已结束");
//				return jo;
//			}
//			//验证会员是否来自该平台
//			if (!user.getCagent().equalsIgnoreCase(cagentEntity.getCagent())) {
//				logger.info("用户:【"+userName+"LuckyDraw】,不属于此平台:【"+cagentEntity.getCagent()+"】的用户");
//				jo.put("status", "faild");
//				jo.put("msg", "会员帐号错误");
//				return jo;
//			}
//			//计算抽奖金额
//			logger.info("单次抽奖金额,最小金额:{},最大金额{}",luckyDrawEntity.getMinamount(),luckyDrawEntity.getMaxamount());
//			float min = luckyDrawEntity.getMinamount();
//			float max = luckyDrawEntity.getMaxamount();
//	        if (min >= max) {
//	            luckyDrawDao.updateStatusByAmount(luckyDrawEntity.getId(), "1");
//	            jo.put("status", "faild");
//	            jo.put("msg", "活动已结束");
//	            return jo;
//	        }
//	        float result = Float.valueOf(new DecimalFormat("##0.00").format(min + Math.random() * (max - min)));//随机金额
//	        
//	        logger.info("用户:【"+userName+"LuckyDraw】,随机到的金额:{}",result);
//
//	        //金额计算方式，0 当日金额  1 昨日金额
//	        String type = luckyDrawEntity.getType();
//			logger.info("抽奖次数计算方式，type:{}",type.equals("1")?"昨日金额":"当日金额");
//			Date begin;
//			Date end;
//
//	        if(type.equals("1")){ //昨日的开始和结束时间
//	            begin = DateUtils.getDayBeginOfYesterday();
//	            end = DateUtils.getDayEndOfYesterDay();
//	        } else {
//	        	begin = DateUtils.getDayBegin();
//	        	end = DateUtils.getDayEnd();
//			}
//
//			String typesOf = luckyDrawEntity.getTypesof(); //红包抽奖类型
//			logger.info("用户:【"+userName+"LuckyDraw】,设置该活动抽奖类型:typesOf:{}",typesOf.equals("1")?"存款金额":"注单总额");
//
//			//查询用户上一次活动抽奖记录,从今天开始到本次活动开始时间
//			List<UserLuckrdrawLogEntity> userLuckyDrawLogs = userLuckrdrawLogDao.selectByUidAndLid(user.getUid(),luckyDrawEntity.getId(),DateUtils.getDayBegin(),beginTime,typesOf);
//			Double usedLimitAmount = 0D;  //用户已用额度
//			if (userLuckyDrawLogs != null && userLuckyDrawLogs.size() > 0) {
//				Optional<UserLuckrdrawLogEntity> maxRecord = userLuckyDrawLogs.stream().max(Comparator.comparingDouble(UserLuckrdrawLogEntity::getUsedBet));
//				usedLimitAmount = maxRecord.get().getUsedBet();
//			}
//			int userLuckyDrawTimes = 0;  //用户存款金额或注单金额匹配的抽奖次数
//			Double usedBet = 0D; //已使用的存款额度或注单总额，用于更新用户红包记录表
//
//	        if ("1".equals(typesOf)) {
//	        	//获取用户充值或加款的金额
//				Double userDidAmount = userTreasureDao.findAllAmountByTime(user.getUid(),DatePatternUtils.dateToStr(begin,DatePatternConstant.NORM_DATETIME_PATTERN),DatePatternUtils.dateToStr(end,DatePatternConstant.NORM_DATETIME_PATTERN));
//				double leftAmount = userDidAmount - usedLimitAmount;
//				logger.info("查询用户:【"+user.getUsername()+"】,充值加款总金额:【"+userDidAmount+"】,抽奖已用额度:【" + usedLimitAmount + "】,剩余额度:【" + leftAmount +"】");
//				//获取剩余额度匹配的活动次数
//				Optional<CagentLuckyDrawDetailEntity> maxTime = details.stream().filter(n -> n.getBalance() <= leftAmount).max(Comparator.comparingDouble(CagentLuckyDrawDetailEntity::getBalance));
//	            if (!maxTime.isPresent()) {
//	                logger.info("用户:【"+userName+"LuckyDraw】,充值金额未达标");
//	                jo.put("status", "faild");
//	                jo.put("msg", "充值金额未达标");
//	                return jo;
//	            }
//				userLuckyDrawTimes = maxTime.get().getTimes();
//	            usedBet = Double.valueOf(maxTime.get().getBalance());
//	        } else if ("2".equals(typesOf)) {
//	            logger.info("查询用户注单金额达标请求参数报文:【"+user.getUsername()+"】,开始时间:【"+begin+"】,结束时间:【"+end+"】");
////	            Double vilidBetAmount = gameBetInfoService.selectUserValidBetAmuontList(user.getUid(), begin, end);
//	            Double vilidBetAmount = gameBetInfoDao.selectUserValidBetAmuontList(user.getUid(), begin, end);
//				double leftAmount = vilidBetAmount - usedLimitAmount;
//				logger.info("查询用户:【"+user.getUsername()+"】,游戏注单总金额:【"+vilidBetAmount+"】,抽奖已用额度:【" + usedLimitAmount + "】,剩余额度:【" + leftAmount +"】");
//				//获取剩余额度匹配的活动次数
//				Optional<CagentLuckyDrawDetailEntity> maxTime = details.stream().filter(n -> n.getValidbetamount() <= leftAmount).max(Comparator.comparingDouble(CagentLuckyDrawDetailEntity::getValidbetamount));
//	            if (!maxTime.isPresent()) {
//					logger.info("用户:【"+userName+"LuckyDraw】,注单金额未达标");
//					jo.put("status", "faild");
//					jo.put("msg", "注单金额未达标");
//					return jo;
//				}
//				userLuckyDrawTimes = maxTime.get().getTimes();
//				usedBet = Double.valueOf(maxTime.get().getValidbetamount());
//	        }
//
//	        logger.info("查询平台抽奖剩余额度");
//	        Map<String, Object> cagentStoredvalue = luckyDrawDao.selectByCidCagentStoredvalue(cagentEntity.getId());
//	        logger.info("平台剩余额度：{}",cagentStoredvalue.get("remainvalue").toString());
//	        if (Float.parseFloat(cagentStoredvalue.get("remainvalue").toString()) < result) {
//	            jo.put("status", "faild");
//	            jo.put("msg", "平台额度已不足");
//	            return jo;
//	        }
//
//	        //查询会员已用抽奖次数
//			List<UserLuckrdrawLogEntity> usedLogs = userLuckrdrawLogDao.selectByUidAndLid(user.getUid(),luckyDrawEntity.getId(),beginTime,now,typesOf);
//
//			Optional<UserLuckrdrawLogEntity> maxRecord = usedLogs.stream().max(Comparator.comparingDouble(UserLuckrdrawLogEntity::getUsedBet));
//			if (maxRecord.isPresent()) {
//				usedBet = usedBet + usedLimitAmount;
//			}
//	        int usedTime = usedLogs.size();
//
//	        logger.info("用户:【"+ userName +"LuckyDraw】,该活动的可用抽奖次数:【"+ userLuckyDrawTimes + "】,已用抽奖次数:【" + usedTime + "】");
//
//	        if (usedTime >= userLuckyDrawTimes) {
//				jo.put("status", "faild");
//				jo.put("msg", "已无抽奖次数");
//				return jo;
//			}
//			float amountUsed = luckyDrawEntity.getAmountused();//获取奖池已用金额
//			float amountLimit = luckyDrawEntity.getAmountlimit();//获取奖池最大金额
//
//			logger.info("奖池：最大金额:{}，已用金额:{}",amountLimit,amountUsed);
//			//奖金池剩余金额
//			float luckyBalance = Math.abs(amountLimit-amountUsed);
//
//			//先判断奖金池的金额是否符合抽奖
//			if(luckyBalance < min){
//				luckyDrawDao.updateStatusByAmount(luckyDrawEntity.getId(), "1");
//				jo.put("status", "faild");
//				jo.put("msg", "活动已结束");
//				return jo;
//			}else {
//				if(result > luckyBalance){
//					result = luckyBalance;
//				}
//			}
//
//			logger.info("更新抽奖已用金额");
//			Map<String, String> hashMap = new HashMap<>();
//			//更新抽奖已用金额
//			hashMap.put("id", String.valueOf(luckyDrawEntity.getId()));
//			hashMap.put("amountUsed", (amountUsed + result)+"");
//			updateLuckydraw(hashMap);
//
//			logger.info("添加抽奖日志：uid:{"+user.getUid().toString()+"},lid:{"+luckyDrawEntity.getId()+"},cid:{"+cagentStoredvalue.get("cid").toString()+"},amount:{"+result+"}");
//
//			//查询用户今日所有抽奖次数
//			int userHasDrawTimes = 0;
//			int userTotalHasDrawTimes = 0;
//			//查询用户最近的一条抽奖记录
//			UserLuckrdrawLogEntity recentRecord = userLuckrdrawLogDao.selectRecentRecord(user.getUid());
//			if (recentRecord != null) {
//				if (recentRecord.getAddtime().after(DateUtils.getDayBegin())) {
//					userHasDrawTimes = recentRecord.getTodaytimes();
//					userTotalHasDrawTimes = recentRecord.getTotaltimes();
//				} else {
//					userTotalHasDrawTimes = recentRecord.getTotaltimes();
//				}
//			}
//
//
//			UserLuckrdrawLogEntity newLuckDarwLog = new UserLuckrdrawLogEntity();
//			//添加抽奖日志
//			newLuckDarwLog.setAddtime(now);
//			newLuckDarwLog.setAmount(result);
//			newLuckDarwLog.setUid(user.getUid());
//			newLuckDarwLog.setLid(luckyDrawEntity.getId());
//			newLuckDarwLog.setCid(cagentEntity.getId());
//			newLuckDarwLog.setOrderid("HB" + System.currentTimeMillis());
//			newLuckDarwLog.setIp(refurl);
//			newLuckDarwLog.setTypesof(typesOf);
//			newLuckDarwLog.setUsedBet(usedBet);
//			newLuckDarwLog.setTodaytimes(userHasDrawTimes + 1);
//			newLuckDarwLog.setTotaltimes(userTotalHasDrawTimes + 1);
//
//			userLuckrdrawLogDao.insertSelective(newLuckDarwLog);
//
//	        jo.put("status", "success");
//	        jo.put("result", result);
//	        jo.put("msg", "正常");
//	        return jo;
//        } catch (Exception e) {
//            e.printStackTrace();
//            logger.info("用户:【"+userName+"LuckyDraw】调用红包抽奖业务异常:{}",e.getMessage());
//            throw new Exception(e.getMessage());
//        }
//	}
	
	
	private JSONObject responseResult(String status,Date nowDate,Date beginTime,Date endTime,String msg){
        JSONObject data = new JSONObject();
        data.put("status",status);
        data.put("now", nowDate);
        data.put("begintime", beginTime);
        data.put("endtime", endTime);
        if(status.equals("waiting")){
            data.put("diff", - nowDate.getTime() - beginTime.getTime() / 1000);
        }
        data.put("msg",msg);
        return data;
    }
	
	private JSONObject responseResult(String status,int luckyDraws,Double usedBet,String msg){
        JSONObject data = new JSONObject();
        data.put("luckyDraws",luckyDraws);
        data.put("usedBet",usedBet);
        data.put("status",status);
        data.put("msg",msg);
        return data;
    }
	
	public static void main(String[] args) {
        Integer a = 123456;
        String s = String.valueOf(a);
        String ss = String.valueOf(a).intern();
        System.err.println(a.toString().equals(s));
        System.err.println(a.toString()==s);
        System.err.println(ss == s);
    }
}
