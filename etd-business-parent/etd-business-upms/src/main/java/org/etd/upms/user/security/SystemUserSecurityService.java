package org.etd.upms.user.security;

import com.etd.framework.starter.client.core.user.IUserService;
import org.etd.framework.common.core.user.PermissionAuthority;
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

/**
 * 专门处理 Security 认证与用户信息加载的适配服务
 * <p>
 * 实现 Security / OAuth 框架的 IUserService 接口，负责在用户登录认证阶段
 * 拼接用户信息、租户有效性校验以及授权角色和数据权限上下文。
 *
 * @author 牛昌
 */
@Service
public class SystemUserSecurityService implements IUserService {

    /**
     * 系统用户基础 Service
     */
    @Autowired
    private SystemUserService userService;

    /**
     * 系统用户安全权限服务（负责角色与数据权限加载）
     */
    @Autowired
    private SystemUserPermissionsService permissionsService;

    /**
     * 租户业务服务（负责租户状态与登录限制校验）
     */
    @Autowired
    private SystemTenantService tenantService;

    /**
     * 根据用户 ID 忽略租户与数据权限拦截地加载登录用户详情
     *
     * @param id 用户 ID
     * @return 登录用户详情 UserDetails
     */
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

    /**
     * 根据登录账号忽略租户与数据权限拦截地加载登录用户详情
     *
     * @param account 登录账号
     * @return 登录用户详情 UserDetails
     */
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

    /**
     * 将系统用户实体与权限模型转换为上下文通用的 UserDetails 对象
     *
     * @param systemUserEntity 用户实体
     * @return 完整填充了角色与数据权限的 UserDetails
     */
    private UserDetails toUserDetails(SystemUserEntity systemUserEntity) {
        UserPermissions permissions = permissionsService.loadPermissionsByUser(systemUserEntity.getId());
        validateTenant(systemUserEntity, permissions);
        UserDetails userDetails = toUserDetails(systemUserEntity, permissions);
        disableUserWhenTenantUnavailable(userDetails);
        return userDetails;
    }

    /**
     * 映射转换实体属性与权限集合到 UserDetails
     *
     * @param systemUserEntity 用户实体
     * @param permissions 权限与数据范围模型
     * @return UserDetails 对象
     */
    private UserDetails toUserDetails(SystemUserEntity systemUserEntity, UserPermissions permissions) {
        UserDetails userDetails = Mappers.getMapper(SystemUserConverter.class).toUserDetails(systemUserEntity);
        userDetails.setTenantId(systemUserEntity.getTenantId());
        userDetails.setRoleCodes(permissions.getRoleCodes());
        userDetails.setAuthorities(permissions.getAuthorityCodes().stream()
                .map(PermissionAuthority::new)
                .toList());
        userDetails.setPlatformAdmin(permissions.getPlatformAdmin());
        userDetails.setTenantAdmin(permissions.getTenantAdmin());
        userDetails.setOrgId(permissions.getPrimaryOrganizationId());
        userDetails.setOrgIds(permissions.getOrganizationIds());
        userDetails.setPermissionTypes(permissions.getPermissionTypes());
        userDetails.setCustomOrgIds(permissions.getCustomOrganizationIds());
        userDetails.setScopeOrgIds(permissions.getScopeOrganizationIds());
        return userDetails;
    }

    /**
     * 当所属租户处于停用或不可登录状态时，禁用非平台管理员账号
     *
     * @param userDetails 用户详情
     */
    private void disableUserWhenTenantUnavailable(UserDetails userDetails) {
        if (!userDetails.isPlatformAdmin() && !tenantService.isLoginEnabled(userDetails.getTenantId())) {
            userDetails.setEnabled(false);
        }
    }

    /**
     * 校验用户实体关联的租户 ID 与角色授权关系对应的租户 ID 是否一致
     *
     * @param user 用户实体
     * @param permissions 权限模型
     */
    private void validateTenant(SystemUserEntity user, UserPermissions permissions) {
        if (user.getTenantId() == null || !user.getTenantId().equals(permissions.getTenantId())) {
            throw new IllegalStateException("用户 " + user.getId() + " 的所属租户与角色关系租户不一致。");
        }
    }
}
