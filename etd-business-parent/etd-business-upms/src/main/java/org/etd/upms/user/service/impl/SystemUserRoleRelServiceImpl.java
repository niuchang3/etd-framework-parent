package org.etd.upms.user.service.impl;

import org.etd.framework.common.core.user.UserPermissions;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.mapper.SystemUserRoleRelMapper;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemUserRoleRelServiceImpl implements SystemUserRoleRelService {

    @Autowired
    private SystemUserRoleRelMapper userRoleRelMapper;

    @Override
    public List<SystemUserRoleVO> selectByUser(Long userId) {
        return userRoleRelMapper.selectByUserId(userId);
    }

    /**
     * 聚合用户全部角色权限码，并校验角色关系只属于一个租户。
     */
    @IgnoreTenant
    @Override
    public UserPermissions loadPermissionsByUser(Long userId) {
        List<SystemUserRoleVO> roles = selectByUser(userId);
        UserPermissions permissions = new UserPermissions();
        if (CollectionUtils.isEmpty(roles)) {
            return permissions;
        }
        permissions.setTenantId(resolveUniqueTenantId(userId, roles));
        Set<String> roleCodes = roles.stream()
                .map(SystemUserRoleVO::getRoleCode)
                .filter(roleCode -> roleCode != null && !roleCode.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        permissions.setRoleCodes(roleCodes);
        permissions.setPlatformAdmin(containsRole(roleCodes, BasicConstant.SystemRole.PLATFORM_ADMIN));
        permissions.setTenantAdmin(containsRole(roleCodes, BasicConstant.SystemRole.TENANT_ADMIN));
        return permissions;
    }

    private Long resolveUniqueTenantId(Long userId, List<SystemUserRoleVO> roles) {
        Set<Long> tenantIds = roles.stream()
                .map(SystemUserRoleVO::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (tenantIds.size() != 1) {
            throw new IllegalStateException("用户 " + userId + " 的角色关系未归属于唯一租户。");
        }
        return tenantIds.iterator().next();
    }

    private boolean containsRole(Set<String> roleCodes, BasicConstant.SystemRole expectedRole) {
        return roleCodes.stream().anyMatch(roleCode -> expectedRole.getCode().equalsIgnoreCase(roleCode));
    }
}
