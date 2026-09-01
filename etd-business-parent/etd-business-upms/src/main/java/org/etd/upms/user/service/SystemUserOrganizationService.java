package org.etd.upms.user.service;

import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;

import java.util.List;
import java.util.Set;

public interface SystemUserOrganizationService {

    Set<Long> selectUserIdsByOrganizationIds(Set<Long> organizationIds);

    List<SystemUserOrganizationVO> selectByUserIds(Set<Long> userIds);

    void replace(Long userId, Set<Long> organizationIds, Long primaryOrganizationId);

    void removeByUserId(Long userId);

    void removeByOrganizationIds(Set<Long> organizationIds);
}
