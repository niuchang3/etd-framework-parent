package org.etd.upms.user.converter;

import org.etd.framework.common.core.user.UserDetails;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.user.controller.vo.SystemUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SystemUserConverter {

    /**
     *
     * @param user
     * @return
     */
    @Mapping(target = "platformAdmin", ignore = true)
    @Mapping(target = "tenantAdmin", ignore = true)
    @Mapping(target = "roleCodes", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "orgId", ignore = true)
    @Mapping(target = "orgIds", ignore = true)
    @Mapping(target = "permissionType", ignore = true)
    @Mapping(target = "permissionTypes", ignore = true)
    @Mapping(target = "customOrgIds", ignore = true)
    @Mapping(target = "scopeOrgIds", ignore = true)
    UserDetails toUserDetails(SystemUserEntity user);

    /**
     * UserDetails 转换UserVo
     * @param userDetails
     * @return
     */
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "dataStatus", ignore = true)
    @Mapping(target = "roleNames", ignore = true)
    @Mapping(target = "organizationNames", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "organizations", ignore = true)
    SystemUserVO toUserVO(UserDetails userDetails);
}
