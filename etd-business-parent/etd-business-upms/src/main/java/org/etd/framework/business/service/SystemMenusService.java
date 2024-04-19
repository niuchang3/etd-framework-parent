package org.etd.framework.business.service;

import org.etd.framework.business.vo.SystemUserMenusVO;

import java.util.List;

public interface SystemMenusService {

    List<SystemUserMenusVO> currentUserMenu();

}
