package org.etd.framework.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.framework.demo.entity.ClassSnap;

/**
 * (ClassSnap)表数据库访问层
 *
 * @author 牛昌
 * @since 2022-09-28 09:55:51
 */
@Mapper
public interface ClassSnapDao extends BaseMapper<ClassSnap> {

}

