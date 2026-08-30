package org.etd.upms.role.service;

import java.util.Map;
import java.util.Set;

public interface SystemRoleMenuService {

    Map<Long, Integer> selectMenuAccessLevels(Set<Long> roleIds);

    void removeByMenuIds(Set<Long> menuIds);
}
