package org.etd.upms.tenant.converter;

import org.etd.upms.tenant.entity.SystemTenantEntity;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 租户模块 StructMapper 转换器。
 */
@Mapper
public interface SystemTenantConvert {

    @Mapping(target = "adminUser", ignore = true)
    SystemTenantVO toVo(SystemTenantEntity entity);


    List<SystemTenantVO> toVo(List<SystemTenantEntity> entity);
}
