package org.etd.upms.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.role.controller.dto.SystemRoleMenuGrantDTO;
import org.etd.upms.role.controller.vo.SystemRoleMenuVO;
import org.etd.upms.role.entity.SystemRoleMenuRelEntity;
import org.etd.upms.role.mapper.SystemRoleMenuRelMapper;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Override
    public List<SystemRoleMenuVO> selectByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleMenuRelEntity::getRoleId, roleId)
                .orderByAsc(SystemRoleMenuRelEntity::getMenuId);
        return roleMenuRelMapper.selectList(wrapper).stream()
                .map(relation -> new SystemRoleMenuVO(relation.getMenuId(), relation.getAccessLevel()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean replace(Long roleId, List<SystemRoleMenuGrantDTO> menus) {
        removeByRoleId(roleId);
        for (SystemRoleMenuGrantDTO menu : menus) {
            SystemRoleMenuRelEntity relation = new SystemRoleMenuRelEntity();
            relation.setRoleId(roleId);
            relation.setMenuId(menu.getMenuId());
            relation.setAccessLevel(menu.getAccessLevel());
            if (roleMenuRelMapper.insert(relation) <= 0) {
                throw new ApiRuntimeException("角色菜单权限保存失败。");
            }
        }
        return true;
    }

    @Override
    public void removeByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleMenuRelEntity::getRoleId, roleId);
        roleMenuRelMapper.delete(wrapper);
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

    @IgnoreTenant
    @Override
    public void removeByTenantAndMenuIds(Long tenantId, Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleMenuRelEntity::getTenantId, tenantId)
                .in(SystemRoleMenuRelEntity::getMenuId, menuIds);
        roleMenuRelMapper.delete(wrapper);
    }
}
