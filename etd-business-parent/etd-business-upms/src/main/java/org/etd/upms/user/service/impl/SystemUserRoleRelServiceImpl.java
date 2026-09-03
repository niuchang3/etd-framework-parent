package org.etd.upms.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.mapper.SystemUserRoleRelMapper;
import org.etd.upms.user.entity.SystemUserRoleRelEntity;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 用户角色关系能力服务，负责角色绑定与关系查询。
 */
@Service
public class SystemUserRoleRelServiceImpl implements SystemUserRoleRelService {

    @Autowired
    private SystemUserRoleRelMapper userRoleRelMapper;

    /**
     * 查询 By User
     *
     * @param userId 参数 userId
     * @return 处理结果
     */
    @Override
    public List<SystemUserRoleVO> selectByUser(Long userId) {
        return userRoleRelMapper.selectByUserId(userId);
    }

    /**
     * 查询 Assignments By User Ids
     *
     * @param userIds 参数 userIds
     * @return 处理结果
     */
    @Override
    public List<SystemUserRoleVO> selectAssignmentsByUserIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRoleRelMapper.selectAssignmentsByUserIds(userIds);
    }

    /**
     * exists By Role Id
     *
     * @param roleId 参数 roleId
     * @return 处理结果
     */
    @Override
    public boolean existsByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemUserRoleRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserRoleRelEntity::getRoleId, roleId);
        return userRoleRelMapper.selectCount(wrapper) > 0;
    }

    /**
     * 分配授权 Role
     *
     * @param tenantId 参数 tenantId
     * @param userId 参数 userId
     * @param roleId 参数 roleId
     */
    @IgnoreTenant
    @Override
    public void assignRole(Long tenantId, Long userId, Long roleId) {
        SystemUserRoleRelEntity relation = new SystemUserRoleRelEntity();
        relation.setTenantId(tenantId);
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        if (userRoleRelMapper.insert(relation) <= 0) {
            throw new ApiRuntimeException("租户管理员角色绑定失败。");
        }
    }

    /**
     * replace
     *
     * @param userId 参数 userId
     * @param roleIds 参数 roleIds
     */
    @Override
    public void replace(Long userId, Set<Long> roleIds) {
        removeByUserId(userId);
        Long tenantId = requireTenantId();
        roleIds.forEach(roleId -> insertRelation(tenantId, userId, roleId));
    }

    /**
     * 移除 By User Id
     *
     * @param userId 参数 userId
     */
    @Override
    public void removeByUserId(Long userId) {
        LambdaQueryWrapper<SystemUserRoleRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserRoleRelEntity::getUserId, userId);
        userRoleRelMapper.delete(wrapper);
    }

    private void insertRelation(Long tenantId, Long userId, Long roleId) {
        SystemUserRoleRelEntity relation = new SystemUserRoleRelEntity();
        relation.setTenantId(tenantId);
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        if (userRoleRelMapper.insert(relation) <= 0) {
            throw new ApiRuntimeException("用户角色绑定失败。");
        }
    }

    private Long requireTenantId() {
        Long tenantId = RequestContext.getTenantCode();
        if (tenantId == null) {
            throw new ApiRuntimeException("用户角色维护时必须指定租户。");
        }
        return tenantId;
    }
}
