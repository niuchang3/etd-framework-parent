package org.etd.upms.menu.service;

import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.constants.MenuType;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.constants.PermissionCode;
import org.etd.framework.starter.mybaits.permission.annotation.IgnoreDataPermission;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.menu.entity.MenuPermissionGrant;
import org.etd.upms.menu.mapper.SystemMenusMapper;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.Set;

/** 菜单权限读取能力，将有效资源授权展开为 Security 使用的具体操作权限。 */
@Service
public class SystemMenuPermissionService {
    private final SystemMenusMapper menusMapper;

    public SystemMenuPermissionService(SystemMenusMapper menusMapper) { this.menusMapper = menusMapper; }

    /** 登录阶段由 SQL 显式限制租户，避免依赖尚未建立的请求身份。 */
    @IgnoreTenant
    @IgnoreDataPermission
    public Set<String> fetchAuthorityCodesByUser(Long userId, Long tenantId, boolean tenantAdmin, boolean platformAdmin) {
        var grants = menusMapper.selectPermissionGrantListByUser(userId, tenantId, tenantAdmin, platformAdmin,
                BasicConstant.DataStatus.ENABLED.getCode(), BasicConstant.AccessLevel.READ_ONLY.getCode(),
                BasicConstant.AccessLevel.READ_WRITE.getCode(), MenuType.DIRECTORY.getCode());
        Set<String> authorities = new LinkedHashSet<>();
        for (MenuPermissionGrant grant : grants) {
            authorities.add(PermissionCode.createAuthority(grant.permissionCode(), PermissionAction.READ));
            if (BasicConstant.AccessLevel.READ_WRITE.getCode().equals(grant.accessLevel())) {
                authorities.add(PermissionCode.createAuthority(grant.permissionCode(), PermissionAction.WRITE));
            }
        }
        return authorities;
    }
}
