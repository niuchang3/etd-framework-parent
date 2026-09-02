package org.etd.upms.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.upms.tenant.entity.SystemTenantEntity;


/**
 * 租户数据访问 Mapper 接口。
 */
@Mapper
public interface SystemTenantMapper extends BaseMapper<SystemTenantEntity> {
}
