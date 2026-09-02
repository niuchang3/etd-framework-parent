package org.etd.upms.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.upms.tenant.entity.SystemTenantMenuRelEntity;

/**
 * 租户与菜单关联关系 Mapper 接口。
 */
@Mapper
public interface SystemTenantMenuRelMapper extends BaseMapper<SystemTenantMenuRelEntity> {
}
