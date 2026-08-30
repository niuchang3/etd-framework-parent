package org.etd.upms.menu.service;

import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.etd.upms.menu.controller.dto.SystemMenuSaveDTO;
import org.etd.upms.menu.controller.vo.SystemMenuVO;

import java.util.List;
import java.util.Set;

public interface SystemMenusService {

    SystemMenuVO selectById(Long id);

    Long insert(SystemMenuSaveDTO dto);

    boolean update(Long id, SystemMenuSaveDTO dto);

    Set<Long> selectSubtreeIds(Long id);

    boolean deleteByIds(Set<Long> ids);

    boolean switchStatus(Long id, Integer status);

    List<SystemUserMenusVO> selectEnabledByIds(Set<Long> menuIds, Long tenantId);

}
