package org.etd.upms.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.role.entity.SystemRoleOrganizationRelEntity;
import org.etd.upms.role.mapper.SystemRoleOrganizationRelMapper;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 角色与组织数据权限关联关系能力 Service 实现类。
 */
@Service
public class SystemRoleOrganizationServiceImpl implements SystemRoleOrganizationService {

    @Autowired
    private SystemRoleOrganizationRelMapper roleOrganizationRelMapper;

    /**
     * 查询 Organization Ids
     *
     * @param roleId 参数 roleId
     * @return 处理结果
     */
    @Override
    public Set<Long> selectOrganizationIds(Long roleId) {
        return selectOrganizationIdsByRoleIds(Set.of(roleId));
    }

    /**
     * 查询 Organization Ids By Role Ids
     *
     * @param roleIds 参数 roleIds
     * @return 处理结果
     */
    @Override
    public Set<Long> selectOrganizationIdsByRoleIds(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<SystemRoleOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleOrganizationRelEntity::getRoleId, roleIds)
                .orderByAsc(SystemRoleOrganizationRelEntity::getOrganizationId)
                .select(SystemRoleOrganizationRelEntity::getOrganizationId);
        Set<Long> organizationIds = new LinkedHashSet<>();
        roleOrganizationRelMapper.selectList(wrapper)
                .forEach(relation -> organizationIds.add(relation.getOrganizationId()));
        return organizationIds;
    }

    /**
     * replace
     *
     * @param roleId 参数 roleId
     * @param organizationIds 参数 organizationIds
     * @return 处理结果
     */
    @Override
    public boolean replace(Long roleId, Set<Long> organizationIds) {
        removeByRoleId(roleId);
        organizationIds.forEach(organizationId -> insertRelation(roleId, organizationId));
        return true;
    }

    /**
     * 移除 By Role Id
     *
     * @param roleId 参数 roleId
     */
    @Override
    public void removeByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemRoleOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleOrganizationRelEntity::getRoleId, roleId);
        roleOrganizationRelMapper.delete(wrapper);
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
        LambdaQueryWrapper<SystemRoleOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleOrganizationRelEntity::getOrganizationId, organizationIds);
        roleOrganizationRelMapper.delete(wrapper);
    }

    private void insertRelation(Long roleId, Long organizationId) {
        SystemRoleOrganizationRelEntity relation = new SystemRoleOrganizationRelEntity();
        relation.setRoleId(roleId);
        relation.setOrganizationId(organizationId);
        if (roleOrganizationRelMapper.insert(relation) <= 0) {
            throw new ApiRuntimeException("角色自定义组织数据权限保存失败。");
        }
    }
}
