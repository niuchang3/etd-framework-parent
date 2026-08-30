package org.etd.upms.user.biz;

import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.menu.service.SystemMenusService;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemUserBizService {

    @Autowired
    private SystemMenusService menusService;

    @Autowired
    private SystemTenantMenuService tenantMenuService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemRoleMenuService roleMenuService;

    public List<SystemUserMenusVO> currentUserMenus() {
        UserDetails user = RequestContext.getUser();
        Long tenantId = user.getTenantId();
        Set<Long> tenantMenuIds = tenantMenuService.selectMenuIds(tenantId);
        // 管理员直接使用租户菜单，普通用户还需要按角色菜单继续过滤。
        if (user.isAdmin()) {
            return menusService.selectEnabledByIds(tenantMenuIds, tenantId);
        }
        return selectRoleMenus(tenantMenuIds, user.getId(), tenantId);
    }

    private List<SystemUserMenusVO> selectRoleMenus(Set<Long> tenantMenuIds, Long userId, Long tenantId) {
        Set<Long> roleIds = userRoleRelService.selectByUser(userId).stream()
                .map(SystemUserRoleVO::getRoleId)
                .collect(Collectors.toSet());
        Map<Long, Integer> accessLevels = new LinkedHashMap<>(roleMenuService.selectMenuAccessLevels(roleIds));
        accessLevels.keySet().retainAll(tenantMenuIds);
        List<SystemUserMenusVO> menus = menusService.selectEnabledByIds(accessLevels.keySet(), tenantId);
        menus.forEach(menu -> menu.setAccessLevel(accessLevels.get(menu.getId())));
        return menus;
    }
}
