package com.cn.tianxia.api.domain.txdata.v2;

import org.apache.ibatis.annotations.Param;

import com.cn.tianxia.api.project.v2.RefererUrlEntity;

/**
 * 
 * @ClassName RefererUrlDao
 * @Description 域名白名单dao
 * @author Hardy
 * @Date 2019年2月6日 下午3:32:23
 * @version 1.0.0
 */
public interface RefererUrlDao {
    int deleteByPrimaryKey(Integer id);

    int insert(RefererUrlEntity record);

    int insertSelective(RefererUrlEntity record);

    RefererUrlEntity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(RefererUrlEntity record);

    int updateByPrimaryKey(RefererUrlEntity record);

    int selectByReferUrl(@Param("referUrl") String referUrl);
    
    int checkReferUrlByCagent(@Param("referUrl") String referUrl,@Param("cagent") String cagent);
}