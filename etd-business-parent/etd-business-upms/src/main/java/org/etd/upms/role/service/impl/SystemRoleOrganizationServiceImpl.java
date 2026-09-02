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

@Service
public class SystemRoleOrganizationServiceImpl implements SystemRoleOrganizationService {

    @Autowired
    private SystemRoleOrganizationRelMapper roleOrganizationRelMapper;

    @Override
    public Set<Long> selectOrganizationIds(Long roleId) {
        return selectOrganizationIdsByRoleIds(Set.of(roleId));
    }

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

    @Override
    public boolean replace(Long roleId, Set<Long> organizationIds) {
        removeByRoleId(roleId);
        organizationIds.forEach(organizationId -> insertRelation(roleId, organizationId));
        return true;
    }

    @Override
    public void removeByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemRoleOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleOrganizationRelEntity::getRoleId, roleId);
        roleOrganizationRelMapper.delete(wrapper);
    }

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
