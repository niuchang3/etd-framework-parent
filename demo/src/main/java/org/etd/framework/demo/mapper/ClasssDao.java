package org.etd.framework.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.etd.framework.demo.entity.Classs;
import org.etd.framework.starter.mybaits.core.mapper.EtdBaseMapper;

/**
 * (Classs)表数据库访问层
 *
 * @author 牛昌
 * @since 2022-09-28 09:55:53
 */
@Mapper
public interface ClasssDao extends EtdBaseMapper<Classs> {

}

