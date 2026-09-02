package org.etd.upms.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.upms.config.entity.SystemConfigEntity;

/**
 * 系统参数配置 MyBatis 数据访问 Mapper 接口。
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigEntity> {
}
