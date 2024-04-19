package org.etd.framework.business.converter;

import com.etd.framework.starter.client.core.TenantAuthority;
import org.etd.framework.business.vo.SystemUserRoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface SystemUserRoleConverter {


    @Mapping(source = "menus", target = "authority")
    TenantAuthority toTenantAuthority(SystemUserRoleVO systemUserRoleVo);


    List<TenantAuthority> toTenantAuthority(List<SystemUserRoleVO> systemUserRoleVo);
}
