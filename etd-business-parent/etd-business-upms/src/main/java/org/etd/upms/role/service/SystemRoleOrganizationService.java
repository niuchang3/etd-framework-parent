package org.etd.upms.role.service;

import java.util.Set;

/**
 * 角色与组织数据权限关联关系能力 Service 接口。
 */
public interface SystemRoleOrganizationService {

    Set<Long> selectOrganizationIds(Long roleId);

    Set<Long> selectOrganizationIdsByRoleIds(Set<Long> roleIds);

    boolean replace(Long roleId, Set<Long> organizationIds);

    void removeByRoleId(Long roleId);

    void removeByOrganizationIds(Set<Long> organizationIds);
}
