package org.etd.upms.menu.service;

import java.util.Set;

/**
 * 菜单与 API 接口权限关联关系 Service 接口。
 */
public interface SystemMenuApiService {

    void removeByMenuIds(Set<Long> menuIds);
}
