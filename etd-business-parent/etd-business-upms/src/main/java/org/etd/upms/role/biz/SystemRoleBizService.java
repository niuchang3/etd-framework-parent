package org.etd.upms.role.biz;

import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.controller.dto.SystemRoleMenuGrantDTO;
import org.etd.upms.role.controller.dto.SystemRoleSaveDTO;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.etd.upms.role.service.SystemRoleService;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemRoleBizService {

    @Autowired
    private SystemRoleService roleService;

    @Autowired
    private SystemRoleMenuService roleMenuService;

    @Autowired
    private SystemRoleOrganizationService roleOrganizationService;

    @Autowired
    private SystemOrganizationService organizationService;

    @Autowired
    private SystemTenantMenuService tenantMenuService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    /**
     * 角色及其自定义组织数据范围必须在同一事务内创建。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insert(SystemRoleSaveDTO dto) {
        Set<Long> organizationIds = resolveOrganizationIds(dto);
        organizationService.requireAllExist(organizationIds);
        Long roleId = roleService.insert(dto);
        roleOrganizationService.replace(roleId, organizationIds);
        return roleId;
    }

    /**
     * 切换为非自定义数据权限时同步清空旧组织关系，避免遗留授权。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long roleId, SystemRoleSaveDTO dto) {
        Set<Long> organizationIds = resolveOrganizationIds(dto);
        organizationService.requireAllExist(organizationIds);
        if (!roleService.update(roleId, dto)) {
            return false;
        }
        return roleOrganizationService.replace(roleId, organizationIds);
    }

    public Set<Long> selectOrganizationIds(Long roleId) {
        roleService.requireExists(roleId);
        return roleOrganizationService.selectOrganizationIds(roleId);
    }

    /**
     * 独立维护组织范围时，仅允许操作自定义跨组织数据权限角色。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceOrganizations(Long roleId, Set<Long> organizationIds) {
        roleService.requireWritable(roleId, "内置角色不允许修改自定义组织数据权限");
        if (!isCustomOrganizationPermission(roleId)) {
            throw new ApiRuntimeException("只有自定义跨组织数据权限角色可以维护组织范围。");
        }
        Set<Long> normalizedIds = normalizeRequiredOrganizationIds(organizationIds);
        organizationService.requireAllExist(normalizedIds);
        return roleOrganizationService.replace(roleId, normalizedIds);
    }

    /**
     * 角色菜单采用全量替换，并严格限制在当前租户的菜单授权范围内。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceMenus(Long roleId, List<SystemRoleMenuGrantDTO> menus) {
        roleService.requireWritable(roleId, "内置角色不允许修改菜单权限");
        ensureNoDuplicateMenus(menus);
        Set<Long> tenantMenuIds = tenantMenuService.selectMenuIds(requireTenantId());
        Set<Long> requestedMenuIds = menus.stream().map(SystemRoleMenuGrantDTO::getMenuId).collect(Collectors.toSet());
        if (!tenantMenuIds.containsAll(requestedMenuIds)) {
            throw new ApiRuntimeException("角色只能授权当前租户已拥有的菜单。");
        }
        return roleMenuService.replace(roleId, menus);
    }

    /**
     * 已分配用户的角色不允许删除，避免用户权限关系被静默破坏。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long roleId) {
        roleService.requireWritable(roleId, "内置角色不允许删除");
        if (userRoleRelService.existsByRoleId(roleId)) {
            throw new ApiRuntimeException("角色已分配给用户，不能删除。");
        }
        roleMenuService.removeByRoleId(roleId);
        roleOrganizationService.removeByRoleId(roleId);
        return roleService.delete(roleId);
    }

    private Set<Long> resolveOrganizationIds(SystemRoleSaveDTO dto) {
        if (!BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode().equals(dto.getPermissionType())) {
            return Set.of();
        }
        return normalizeRequiredOrganizationIds(dto.getOrganizationIds());
    }

    private Set<Long> normalizeRequiredOrganizationIds(Set<Long> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty() || organizationIds.contains(null)) {
            throw new ApiRuntimeException("自定义跨组织数据权限至少需要选择一个组织。");
        }
        return new LinkedHashSet<>(organizationIds);
    }

    private boolean isCustomOrganizationPermission(Long roleId) {
        return BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode()
                .equals(roleService.selectById(roleId).getPermissionType());
    }

    private void ensureNoDuplicateMenus(List<SystemRoleMenuGrantDTO> menus) {
        long uniqueCount = menus.stream().map(SystemRoleMenuGrantDTO::getMenuId).distinct().count();
        if (uniqueCount != menus.size()) {
            throw new ApiRuntimeException("菜单授权列表中不能包含重复菜单。");
        }
    }

    private Long requireTenantId() {
        Long tenantId = RequestContext.getTenantCode();
        if (tenantId == null) {
            throw new ApiRuntimeException("角色维护时必须指定租户。");
        }
        return tenantId;
    }
}
