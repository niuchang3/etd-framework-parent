package org.etd.upms.converter;

import org.etd.upms.entity.SystemTenantEntity;
import org.etd.upms.controller.tenant.vo.SystemTenantVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface SystemTenantConvert {

    SystemTenantVO toVo(SystemTenantEntity entity);


    List<SystemTenantVO> toVo(List<SystemTenantEntity> entity);
}
