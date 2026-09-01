package org.etd.upms.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;
import org.etd.upms.user.entity.SystemUserOrganizationRelEntity;
import org.etd.upms.user.mapper.SystemUserOrganizationRelMapper;
import org.etd.upms.user.service.SystemUserOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SystemUserOrganizationServiceImpl implements SystemUserOrganizationService {

    @Autowired
    private SystemUserOrganizationRelMapper userOrganizationRelMapper;

    @Override
    public Set<Long> selectUserIdsByOrganizationIds(Set<Long> organizationIds) {
        if (organizationIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<SystemUserOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemUserOrganizationRelEntity::getOrganizationId, organizationIds)
                .select(SystemUserOrganizationRelEntity::getUserId);
        Set<Long> userIds = new LinkedHashSet<>();
        userOrganizationRelMapper.selectList(wrapper).forEach(relation -> userIds.add(relation.getUserId()));
        return userIds;
    }

    @Override
    public List<SystemUserOrganizationVO> selectByUserIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userOrganizationRelMapper.selectByUserIds(userIds);
    }

    @Override
    public void replace(Long userId, Set<Long> organizationIds, Long primaryOrganizationId) {
        removeByUserId(userId);
        Long tenantId = requireTenantId();
        organizationIds.forEach(organizationId ->
                insertRelation(tenantId, userId, organizationId, organizationId.equals(primaryOrganizationId)));
    }

    @Override
    public void removeByUserId(Long userId) {
        LambdaQueryWrapper<SystemUserOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserOrganizationRelEntity::getUserId, userId);
        userOrganizationRelMapper.delete(wrapper);
    }

    @Override
    public void removeByOrganizationIds(Set<Long> organizationIds) {
        if (organizationIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemUserOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemUserOrganizationRelEntity::getOrganizationId, organizationIds);
        userOrganizationRelMapper.delete(wrapper);
    }

    private void insertRelation(Long tenantId, Long userId, Long organizationId, boolean primaryOrganization) {
        SystemUserOrganizationRelEntity relation = new SystemUserOrganizationRelEntity();
        relation.setTenantId(tenantId);
        relation.setUserId(userId);
        relation.setOrganizationId(organizationId);
        relation.setPrimaryOrganization(primaryOrganization);
        if (userOrganizationRelMapper.insert(relation) <= 0) {
            throw new ApiRuntimeException("用户组织绑定失败。");
        }
    }

    private Long requireTenantId() {
        Long tenantId = RequestContext.getTenantCode();
        if (tenantId == null) {
            throw new ApiRuntimeException("用户组织维护时必须指定租户。");
        }
        return tenantId;
    }
}
