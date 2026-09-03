package org.etd.upms.user.service;

import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import java.util.List;
import java.util.Set;

/**
 * 用户角色关系能力，权限聚合由安全权限服务统一负责。
 */
public interface SystemUserRoleRelService {


    /**
     * 根据用户ID查询角色相关信息
     * @param userId
     * @return
     */
    List<SystemUserRoleVO> selectByUser(Long userId);

    List<SystemUserRoleVO> selectAssignmentsByUserIds(Set<Long> userIds);

    boolean existsByRoleId(Long roleId);

    void assignRole(Long tenantId, Long userId, Long roleId);

    void replace(Long userId, Set<Long> roleIds);

    void removeByUserId(Long userId);

}
