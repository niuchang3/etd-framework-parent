package org.etd.upms.user.security;

import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.PermissionsService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.user.RoleAuthority;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.user.UserPermissions;
import org.etd.framework.starter.mybaits.permission.annotation.IgnoreDataPermission;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.upms.user.converter.SystemUserConverter;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.user.service.SystemUserService;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Set;

/**
 * 专门处理 Security 认证与用户信息加载的适配服务。
 */
@Service
public class SystemUserSecurityService implements IUserService {

    @Autowired
    private SystemUserService userService;

    @Autowired
    private PermissionsService permissionsService;

    @Autowired
    private SystemTenantService tenantService;



    @IgnoreTenant
    @IgnoreDataPermission
    @Override
    public UserDetails loadUserById(Long id) {
        SystemUserEntity systemUserEntity = userService.selectByUserById(id);
        if (ObjectUtils.isEmpty(systemUserEntity)) {
            return null;
        }
        return toUserDetails(systemUserEntity);
    }

    @IgnoreTenant
    @IgnoreDataPermission
    @Override
    public UserDetails loadUserByAccount(String account) {
        SystemUserEntity systemUserEntity = userService.selectByAccount(account);
        if (ObjectUtils.isEmpty(systemUserEntity)) {
            return null;
        }
        return toUserDetails(systemUserEntity);
    }

    private UserDetails toUserDetails(SystemUserEntity systemUserEntity) {
        UserPermissions permissions = permissionsService.loadPermissionsByUser(systemUserEntity.getId());
        validateTenant(systemUserEntity, permissions);
        UserDetails userDetails = toUserDetails(systemUserEntity, permissions);
        disableUserWhenTenantUnavailable(userDetails);
        return userDetails;
    }

    private UserDetails toUserDetails(SystemUserEntity systemUserEntity, UserPermissions permissions) {
        UserDetails userDetails = Mappers.getMapper(SystemUserConverter.class).toUserDetails(systemUserEntity);
        userDetails.setTenantId(systemUserEntity.getTenantId());
        userDetails.setRoleCodes(permissions.getRoleCodes());
        userDetails.setAuthorities(permissions.getRoleCodes().stream()
                .map(RoleAuthority::new)
                .toList());
        userDetails.setPlatformAdmin(permissions.getPlatformAdmin());
        userDetails.setTenantAdmin(permissions.getTenantAdmin());
        userDetails.setOrgId(permissions.getPrimaryOrganizationId());
        userDetails.setOrgIds(permissions.getOrganizationIds());
        userDetails.setPermissionTypes(permissions.getPermissionTypes());
        userDetails.setPermissionType(resolveLegacyPermissionType(permissions.getPermissionTypes()));
        userDetails.setCustomOrgIds(permissions.getCustomOrganizationIds());
        userDetails.setScopeOrgIds(permissions.getScopeOrganizationIds());
        return userDetails;
    }

    private String resolveLegacyPermissionType(Set<String> permissionTypes) {
        if (permissionTypes.contains(BasicConstant.PermissionType.ALL.getCode())) {
            return BasicConstant.PermissionType.ALL.getCode();
        }
        return permissionTypes.size() == 1 ? permissionTypes.iterator().next() : null;
    }

    private void disableUserWhenTenantUnavailable(UserDetails userDetails) {
        if (!userDetails.isPlatformAdmin() && !tenantService.isLoginEnabled(userDetails.getTenantId())) {
            userDetails.setEnabled(false);
        }
    }

    private void validateTenant(SystemUserEntity user, UserPermissions permissions) {
        if (user.getTenantId() == null || !user.getTenantId().equals(permissions.getTenantId())) {
            throw new IllegalStateException("用户 " + user.getId() + " 的所属租户与角色关系租户不一致。");
        }
    }
}
