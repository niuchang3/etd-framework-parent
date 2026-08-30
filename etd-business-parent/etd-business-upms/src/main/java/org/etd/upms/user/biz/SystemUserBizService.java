package org.etd.upms.user.biz;

import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.menu.service.SystemMenusService;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemUserBizService {

    @Autowired
    private SystemMenusService menusService;

    @Autowired
    private SystemTenantService tenantService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    public List<SystemUserMenusVO> currentUserMenus() {
        SystemTenantVO tenantVO = tenantService.selectCurrentTenant();
        List<SystemUserMenusVO> tenantMenus = menusService.filterMenu(tenantVO.getMenus(), tenantVO.getId());
        UserDetails user = RequestContext.getUser();
        // 管理员直接使用租户菜单，普通用户还需要按角色菜单继续过滤。
        if (user.isAdmin()) {
            return tenantMenus;
        }
        return filterRoleMenus(tenantMenus, user.getId(), user.getTenantId());
    }

    private List<SystemUserMenusVO> filterRoleMenus(List<SystemUserMenusVO> tenantMenus, Long userId, Long tenantId) {
        List<SystemUserRoleVO> userRoleVOS = userRoleRelService.selectByUser(userId);
        StringBuilder menus = new StringBuilder();
        userRoleVOS.stream()
                .map(SystemUserRoleVO::getMenus)
                .filter(roleMenus -> roleMenus != null && !roleMenus.isBlank())
                .forEach(roleMenus -> menus.append(roleMenus).append(","));
        return menusService.filterMenu(tenantMenus, menus.toString(), tenantId);
    }
}
