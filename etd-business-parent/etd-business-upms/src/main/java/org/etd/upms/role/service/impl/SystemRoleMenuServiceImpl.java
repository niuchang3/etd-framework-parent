package org.etd.upms.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.role.entity.SystemRoleMenuRelEntity;
import org.etd.upms.role.mapper.SystemRoleMenuRelMapper;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SystemRoleMenuServiceImpl implements SystemRoleMenuService {

    @Autowired
    private SystemRoleMenuRelMapper roleMenuRelMapper;

    @Override
    public Map<Long, Integer> selectMenuAccessLevels(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleMenuRelEntity::getRoleId, roleIds)
                .eq(SystemRoleMenuRelEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode())
                .select(SystemRoleMenuRelEntity::getMenuId, SystemRoleMenuRelEntity::getAccessLevel);
        Map<Long, Integer> accessLevels = new LinkedHashMap<>();
        roleMenuRelMapper.selectList(wrapper).forEach(relation -> accessLevels.merge(
                relation.getMenuId(), relation.getAccessLevel(), Math::max));
        return accessLevels;
    }

    @IgnoreTenant
    @Override
    public void removeByMenuIds(Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleMenuRelEntity::getMenuId, menuIds);
        roleMenuRelMapper.delete(wrapper);
    }
}
