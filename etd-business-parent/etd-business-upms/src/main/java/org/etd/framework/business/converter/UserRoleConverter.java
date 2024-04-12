package org.etd.framework.business.converter;

import com.etd.framework.starter.client.core.TenantAuthority;
import org.etd.framework.business.vo.UserRoleVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface UserRoleConverter {


    @Mapping(source = "menus", target = "authority")
    TenantAuthority toTenantAuthority(UserRoleVo userRoleVo);


    List<TenantAuthority> toTenantAuthority(List<UserRoleVo> userRoleVo);
}
