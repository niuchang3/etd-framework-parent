package org.etd.upms.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.user.UserPermissions;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;
import org.etd.upms.user.mapper.SystemUserRoleRelMapper;
import org.etd.upms.user.entity.SystemUserRoleRelEntity;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.etd.upms.user.service.SystemUserOrganizationService;
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

    @Autowired
    private SystemUserOrganizationService userOrganizationService;

    @Autowired
    private SystemRoleOrganizationService roleOrganizationService;

    @Autowired
    private SystemOrganizationService organizationService;

    @Override
    public List<SystemUserRoleVO> selectByUser(Long userId) {
        return userRoleRelMapper.selectByUserId(userId);
    }

    @Override
    public List<SystemUserRoleVO> selectAssignmentsByUserIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRoleRelMapper.selectAssignmentsByUserIds(userIds);
    }

    @Override
    public boolean existsByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemUserRoleRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserRoleRelEntity::getRoleId, roleId);
        return userRoleRelMapper.selectCount(wrapper) > 0;
    }

    @IgnoreTenant
    @Override
    public void assignRole(Long tenantId, Long userId, Long roleId) {
        SystemUserRoleRelEntity relation = new SystemUserRoleRelEntity();
        relation.setTenantId(tenantId);
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        if (userRoleRelMapper.insert(relation) <= 0) {
            throw new ApiRuntimeException("租户管理员角色绑定失败。");
        }
    }

    @Override
    public void replace(Long userId, Set<Long> roleIds) {
        removeByUserId(userId);
        Long tenantId = requireTenantId();
        roleIds.forEach(roleId -> insertRelation(tenantId, userId, roleId));
    }

    @Override
    public void removeByUserId(Long userId) {
        LambdaQueryWrapper<SystemUserRoleRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserRoleRelEntity::getUserId, userId);
        userRoleRelMapper.delete(wrapper);
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
        populateDataPermissions(userId, roles, permissions);
        return permissions;
    }

    private void populateDataPermissions(Long userId, List<SystemUserRoleVO> roles, UserPermissions permissions) {
        Set<String> permissionTypes = selectPermissionTypes(roles);
        permissions.setPermissionTypes(permissionTypes);
        List<SystemUserOrganizationVO> organizations = userOrganizationService.selectByUserIds(Set.of(userId));
        Long primaryOrganizationId = selectPrimaryOrganizationId(organizations);
        permissions.setPrimaryOrganizationId(primaryOrganizationId);
        permissions.setOrganizationIds(organizations.stream()
                .map(SystemUserOrganizationVO::getOrganizationId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Set<Long> customOrganizationIds = selectCustomOrganizationIds(roles);
        permissions.setCustomOrganizationIds(customOrganizationIds);
        permissions.setScopeOrganizationIds(resolveScopeOrganizationIds(
                permissionTypes, primaryOrganizationId, customOrganizationIds));
    }

    private Set<String> selectPermissionTypes(List<SystemUserRoleVO> roles) {
        return roles.stream()
                .map(SystemUserRoleVO::getPermissionType)
                .filter(permissionType -> permissionType != null && !permissionType.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Long selectPrimaryOrganizationId(List<SystemUserOrganizationVO> organizations) {
        return organizations.stream()
                .filter(organization -> Boolean.TRUE.equals(organization.getPrimaryOrganization()))
                .map(SystemUserOrganizationVO::getOrganizationId)
                .findFirst()
                .orElse(null);
    }

    private Set<Long> selectCustomOrganizationIds(List<SystemUserRoleVO> roles) {
        Set<Long> customRoleIds = roles.stream()
                .filter(role -> BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode()
                        .equals(role.getPermissionType()))
                .map(SystemUserRoleVO::getRoleId)
                .collect(Collectors.toSet());
        return roleOrganizationService.selectOrganizationIdsByRoleIds(customRoleIds);
    }

    private Set<Long> resolveScopeOrganizationIds(Set<String> permissionTypes, Long primaryOrganizationId,
                                                   Set<Long> customOrganizationIds) {
        Set<Long> scopeOrganizationIds = new LinkedHashSet<>(customOrganizationIds);
        if (primaryOrganizationId == null) {
            return scopeOrganizationIds;
        }
        if (permissionTypes.contains(BasicConstant.PermissionType.ORGANIZATION.getCode())) {
            scopeOrganizationIds.add(primaryOrganizationId);
        }
        if (permissionTypes.contains(BasicConstant.PermissionType.ORGANIZATION_AND_SUBORDINATE.getCode())) {
            scopeOrganizationIds.addAll(organizationService.selectSubtreeIds(primaryOrganizationId));
        }
        return scopeOrganizationIds;
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

    private void insertRelation(Long tenantId, Long userId, Long roleId) {
        SystemUserRoleRelEntity relation = new SystemUserRoleRelEntity();
        relation.setTenantId(tenantId);
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        if (userRoleRelMapper.insert(relation) <= 0) {
            throw new ApiRuntimeException("用户角色绑定失败。");
        }
    }

    private Long requireTenantId() {
        Long tenantId = RequestContext.getTenantCode();
        if (tenantId == null) {
            throw new ApiRuntimeException("用户角色维护时必须指定租户。");
        }
        return tenantId;
    }
}
