package org.etd.upms.menu.biz;

import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.menu.controller.dto.SystemMenuSaveDTO;
import org.etd.upms.menu.service.SystemMenuApiService;
import org.etd.upms.menu.service.SystemMenusService;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Set;

@Service
public class SystemMenuBizService {

    @Autowired
    private SystemMenusService menusService;

    @Autowired
    private SystemTenantMenuService tenantMenuService;

    @Autowired
    private SystemRoleMenuService roleMenuService;

    @Autowired
    private SystemMenuApiService menuApiService;

    /**
     * 新菜单必须同时加入当前租户权限，任一步失败都回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insert(SystemMenuSaveDTO dto) {
        Long tenantId = requireTenantId();
        Long menuId = menusService.insert(dto);
        if (!tenantMenuService.appendMenu(tenantId, menuId)) {
            throw new ApiRuntimeException("菜单写入租户权限失败。");
        }
        return menuId;
    }

    /**
     * 删除菜单子树时同步清理当前租户的菜单权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        Long tenantId = requireTenantId();
        Set<Long> menuIds = menusService.selectSubtreeIds(id);
        if (menuIds.isEmpty()) {
            return false;
        }
        // 菜单是全局资源，删除前必须清理所有租户、角色和接口关联，避免遗留无效关系。
        tenantMenuService.removeByMenuIds(menuIds);
        roleMenuService.removeByMenuIds(menuIds);
        menuApiService.removeByMenuIds(menuIds);
        if (!menusService.deleteByIds(menuIds)) {
            throw new ApiRuntimeException("菜单级联删除失败。");
        }
        return true;
    }

    private Long requireTenantId() {
        Long tenantId = RequestContext.getTenantCode();
        if (ObjectUtils.isEmpty(tenantId)) {
            throw new ApiRuntimeException("菜单维护时必须指定租户。");
        }
        return tenantId;
    }
}
