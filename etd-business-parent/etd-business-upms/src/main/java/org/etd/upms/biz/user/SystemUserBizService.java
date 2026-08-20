package org.etd.upms.biz.user;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.upms.controller.tenant.vo.SystemTenantVO;
import org.etd.upms.controller.user.vo.SystemUserMenusVO;
import org.etd.upms.controller.user.vo.SystemUserRoleVO;
import org.etd.upms.service.menu.SystemMenusService;
import org.etd.upms.service.tenant.SystemTenantService;
import org.etd.upms.service.user.SystemUserRoleRelService;
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
        if (user.isPlatformAdmin() || user.isTenantAdmin()) {
            return tenantMenus;
        }
        return filterRoleMenus(tenantMenus, user.getId());
    }

    private List<SystemUserMenusVO> filterRoleMenus(List<SystemUserMenusVO> tenantMenus, Long userId) {
        List<SystemUserRoleVO> userRoleVOS = userRoleRelService.selectByUser(userId);
        StringBuilder menus = new StringBuilder();
        userRoleVOS.forEach(userRoleVO -> menus.append(userRoleVO.getMenus()).append(","));
        return menusService.filterMenu(tenantMenus, menus.toString(), userRoleVOS.get(0).getTenantId());
    }
}
