package org.etd.upms.role.service;

import org.etd.upms.role.controller.dto.SystemRoleMenuGrantDTO;
import org.etd.upms.role.controller.vo.SystemRoleMenuVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SystemRoleMenuService {

    Map<Long, Integer> selectMenuAccessLevels(Set<Long> roleIds);

    List<SystemRoleMenuVO> selectByRoleId(Long roleId);

    boolean replace(Long roleId, List<SystemRoleMenuGrantDTO> menus);

    void removeByRoleId(Long roleId);

    void removeByMenuIds(Set<Long> menuIds);

    void removeByTenantAndMenuIds(Long tenantId, Set<Long> menuIds);
}
