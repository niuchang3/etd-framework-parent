package org.etd.upms.role.biz;

import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.role.controller.dto.SystemRoleMenuGrantDTO;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.role.service.SystemRoleService;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private SystemTenantMenuService tenantMenuService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    /**
     * 角色菜单采用全量替换，并严格限制在当前租户的菜单授权范围内。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceMenus(Long roleId, List<SystemRoleMenuGrantDTO> menus) {
        roleService.requireExists(roleId);
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
        roleService.requireExists(roleId);
        if (userRoleRelService.existsByRoleId(roleId)) {
            throw new ApiRuntimeException("角色已分配给用户，不能删除。");
        }
        roleMenuService.removeByRoleId(roleId);
        return roleService.delete(roleId);
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
