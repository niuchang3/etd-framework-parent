package org.etd.upms.user.security;

import com.etd.framework.starter.client.core.user.PermissionsService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.user.UserPermissions;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.service.SystemUserOrganizationService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.etd.framework.starter.mybaits.permission.resolver.OrgSubtreeResolver;
import java.util.Collections;

/**
 * 系统用户安全权限服务
 * <p>
 * 专职承载用户角色权限、数据权限范围（如部门及下级部门）的组装与计算逻辑，
 * 实现与 Security / OAuth 鉴权及数据隔离框架的解耦对接。
 *
 * @author 牛昌
 */
@Service
public class SystemUserPermissionsService implements PermissionsService, OrgSubtreeResolver {

    /**
     * 实现 OrgSubtreeResolver SPI 接口，为底层数据权限拦截器提供通用组织子树解析能力
     *
     * @param organizationId 组织 ID
     * @return 节点及其下级子节点 ID 集合
     */
    @Override
    public Set<Long> selectSubtreeIds(Long organizationId) {
        if (organizationId == null) {
            return Collections.emptySet();
        }
        return organizationService.selectSubtreeIds(organizationId);
    }

    /**
     * 用户角色关联服务
     */
    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    /**
     * 用户组织机构关联服务
     */
    @Autowired
    private SystemUserOrganizationService userOrganizationService;

    /**
     * 角色组织机构关联服务
     */
    @Autowired
    private SystemRoleOrganizationService roleOrganizationService;

    /**
     * 组织机构服务
     */
    @Autowired
    private SystemOrganizationService organizationService;

    /**
     * 聚合用户全部角色权限码，计算数据权限范围，并校验角色关系租户一致性。
     *
     * @param userId 用户 ID
     * @return 组装完成的用户权限模型对象 UserPermissions
     */
    @IgnoreTenant
    @Override
    public UserPermissions loadPermissionsByUser(Long userId) {
        List<SystemUserRoleVO> roles = userRoleRelService.selectByUser(userId);
        UserPermissions permissions = new UserPermissions();
        if (CollectionUtils.isEmpty(roles)) {
            return permissions;
        }
        // 校验并锁定用户角色的唯一所属租户
        permissions.setTenantId(resolveUniqueTenantId(userId, roles));
        
        // 收集所有有效角色的权限编码
        Set<String> roleCodes = roles.stream()
                .map(SystemUserRoleVO::getRoleCode)
                .filter(roleCode -> roleCode != null && !roleCode.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        permissions.setRoleCodes(roleCodes);
        permissions.setPlatformAdmin(containsRole(roleCodes, BasicConstant.SystemRole.PLATFORM_ADMIN));
        permissions.setTenantAdmin(containsRole(roleCodes, BasicConstant.SystemRole.TENANT_ADMIN));
        
        // 填充用户对应的数据权限范围（主部门、关联部门、自定义部门及下级部门树）
        populateDataPermissions(userId, roles, permissions);
        return permissions;
    }

    /**
     * 组装并计算用户的数据权限隔离范围信息
     *
     * @param userId 用户 ID
     * @param roles 用户拥有的角色列表
     * @param permissions 待填充的权限模型对象
     */
    private void populateDataPermissions(Long userId, List<SystemUserRoleVO> roles, UserPermissions permissions) {
        // 获取所有角色绑定的数据权限类型（如：仅本人、本部门、本部门及下级、自定义部门等）
        Set<String> permissionTypes = selectPermissionTypes(roles);
        permissions.setPermissionTypes(permissionTypes);

        // 获取用户关联的所有组织信息，并解析出主部门 ID
        List<SystemUserOrganizationVO> organizations = userOrganizationService.selectByUserIds(Set.of(userId));
        Long primaryOrganizationId = selectPrimaryOrganizationId(organizations);
        permissions.setPrimaryOrganizationId(primaryOrganizationId);
        permissions.setOrganizationIds(organizations.stream()
                .map(SystemUserOrganizationVO::getOrganizationId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        // 收集自定义数据权限绑定的组织 ID 集合
        Set<Long> customOrganizationIds = selectCustomOrganizationIds(roles);
        permissions.setCustomOrganizationIds(customOrganizationIds);

        // 计算最终合并后的有效可访问组织/部门 ID 集合
        permissions.setScopeOrganizationIds(resolveScopeOrganizationIds(
                permissionTypes, permissions.getOrganizationIds(), customOrganizationIds));
    }

    /**
     * 提取角色列表中所有有效的数据权限类型编码
     *
     * @param roles 角色列表
     * @return 数据权限类型编码集合
     */
    private Set<String> selectPermissionTypes(List<SystemUserRoleVO> roles) {
        return roles.stream()
                .map(SystemUserRoleVO::getPermissionType)
                .filter(permissionType -> permissionType != null && !permissionType.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 从用户组织列表中筛选出主部门 ID
     *
     * @param organizations 用户组织关联列表
     * @return 主部门 ID（若无则返回 null）
     */
    private Long selectPrimaryOrganizationId(List<SystemUserOrganizationVO> organizations) {
        return organizations.stream()
                .filter(organization -> Boolean.TRUE.equals(organization.getPrimaryOrganization()))
                .map(SystemUserOrganizationVO::getOrganizationId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询角色列表中配置为“自定义数据权限”所绑定的组织机构 ID 集合
     *
     * @param roles 角色列表
     * @return 自定义组织的 ID 集合
     */
    private Set<Long> selectCustomOrganizationIds(List<SystemUserRoleVO> roles) {
        Set<Long> customRoleIds = roles.stream()
                .filter(role -> BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode()
                        .equals(role.getPermissionType()))
                .map(SystemUserRoleVO::getRoleId)
                .collect(Collectors.toSet());
        return roleOrganizationService.selectOrganizationIdsByRoleIds(customRoleIds);
    }

    /**
     * 合并全部所属部门与角色自定义授权，生成不受页面选中节点影响的完整组织权限范围。
     */
    private Set<Long> resolveScopeOrganizationIds(Set<String> permissionTypes, Set<Long> organizationIds,
                                                   Set<Long> customOrganizationIds) {
        // 自定义权限仅包含明确授权的节点，不自动展开其下级。
        Set<Long> scopeOrganizationIds = new LinkedHashSet<>(customOrganizationIds);
        if (permissionTypes.contains(BasicConstant.PermissionType.ORGANIZATION_AND_SUBORDINATE.getCode())) {
            for (Long organizationId : organizationIds) {
                scopeOrganizationIds.addAll(organizationService.selectSubtreeIds(organizationId));
            }
        } else if (permissionTypes.contains(BasicConstant.PermissionType.ORGANIZATION.getCode())) {
            scopeOrganizationIds.addAll(organizationIds);
        }
        return scopeOrganizationIds;
    }

    /**
     * 校验并确保用户所有绑定的角色只属于同一个唯一租户
     *
     * @param userId 用户 ID
     * @param roles 角色列表
     * @return 校验通过的租户 ID
     */
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

    /**
     * 校验角色编码集合中是否包含指定的系统角色
     *
     * @param roleCodes 角色编码集合
     * @param expectedRole 目标期望角色
     * @return 是否包含
     */
    private boolean containsRole(Set<String> roleCodes, BasicConstant.SystemRole expectedRole) {
        return roleCodes.stream().anyMatch(roleCode -> expectedRole.getCode().equalsIgnoreCase(roleCode));
    }
}
