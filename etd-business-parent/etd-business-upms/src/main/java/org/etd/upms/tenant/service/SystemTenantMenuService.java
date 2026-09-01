package org.etd.upms.tenant.service;

import java.util.Set;

public interface SystemTenantMenuService {

    Set<Long> selectMenuIds(Long tenantId);

    boolean appendMenu(Long tenantId, Long menuId);

    boolean replace(Long tenantId, Set<Long> menuIds);

    void removeByMenuIds(Set<Long> menuIds);
}
