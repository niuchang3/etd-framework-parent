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

/**
 * 用户与所属组织关联关系能力 Service 实现类。
 */
@Service
public class SystemUserOrganizationServiceImpl implements SystemUserOrganizationService {

    @Autowired
    private SystemUserOrganizationRelMapper userOrganizationRelMapper;

    /**
     * 查询 User Ids By Organization Ids
     *
     * @param organizationIds 参数 organizationIds
     * @return 处理结果
     */
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

    /**
     * 查询 By User Ids
     *
     * @param userIds 参数 userIds
     * @return 处理结果
     */
    @Override
    public List<SystemUserOrganizationVO> selectByUserIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userOrganizationRelMapper.selectByUserIds(userIds);
    }

    /**
     * replace
     *
     * @param userId 参数 userId
     * @param organizationIds 参数 organizationIds
     * @param primaryOrganizationId 参数 primaryOrganizationId
     */
    @Override
    public void replace(Long userId, Set<Long> organizationIds, Long primaryOrganizationId) {
        removeByUserId(userId);
        Long tenantId = requireTenantId();
        organizationIds.forEach(organizationId ->
                insertRelation(tenantId, userId, organizationId, organizationId.equals(primaryOrganizationId)));
    }

    /**
     * 移除 By User Id
     *
     * @param userId 参数 userId
     */
    @Override
    public void removeByUserId(Long userId) {
        LambdaQueryWrapper<SystemUserOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserOrganizationRelEntity::getUserId, userId);
        userOrganizationRelMapper.delete(wrapper);
    }

    /**
     * 移除 By Organization Ids
     *
     * @param organizationIds 参数 organizationIds
     */
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
