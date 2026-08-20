package org.etd.upms.service.menu;

import org.etd.upms.controller.user.vo.SystemUserMenusVO;

import java.util.List;

public interface SystemMenusService {

    List<SystemUserMenusVO> filterMenu(String menuIds, Long tenantId);

    List<SystemUserMenusVO> filterMenu(List<SystemUserMenusVO> systemAllMenus, String menuIds, Long tenantId);

}
