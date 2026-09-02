package org.etd.upms.user.service;

import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;

import java.util.List;
import java.util.Set;

/**
 * 用户与所属组织关联关系能力 Service 接口。
 */
public interface SystemUserOrganizationService {

    Set<Long> selectUserIdsByOrganizationIds(Set<Long> organizationIds);

    List<SystemUserOrganizationVO> selectByUserIds(Set<Long> userIds);

    void replace(Long userId, Set<Long> organizationIds, Long primaryOrganizationId);

    void removeByUserId(Long userId);

    void removeByOrganizationIds(Set<Long> organizationIds);
}
