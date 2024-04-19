package org.etd.framework.business.converter;

import org.etd.framework.business.entity.SystemTenantEntity;
import org.etd.framework.business.vo.SystemTenantVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface SystemTenantConvert {

    SystemTenantVO toVo(SystemTenantEntity entity);


    List<SystemTenantVO> toVo(List<SystemTenantEntity> entity);
}
