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

/**
 * 角色与菜单关联关系能力 Service 实现类。
 */
@Service
public class SystemRoleMenuServiceImpl implements SystemRoleMenuService {

    @Autowired
    private SystemRoleMenuRelMapper roleMenuRelMapper;

    /**
     * 查询 Menu Access Levels
     *
     * @param roleIds 参数 roleIds
     * @return 处理结果
     */
    @Override
    public Map<Long, String> selectMenuAccessLevels(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleMenuRelEntity::getRoleId, roleIds)
                .eq(SystemRoleMenuRelEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode())
                .select(SystemRoleMenuRelEntity::getMenuId, SystemRoleMenuRelEntity::getAccessLevel);
        Map<Long, String> accessLevels = new LinkedHashMap<>();
        roleMenuRelMapper.selectList(wrapper).forEach(relation -> accessLevels.merge(
                relation.getMenuId(), relation.getAccessLevel(), BasicConstant.AccessLevel::merge));
        return accessLevels;
    }

    /**
     * 查询 By Role Id
     *
     * @param roleId 参数 roleId
     * @return 处理结果
     */
    @Override
    public List<SystemRoleMenuVO> selectByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleMenuRelEntity::getRoleId, roleId)
                .orderByAsc(SystemRoleMenuRelEntity::getMenuId);
        return roleMenuRelMapper.selectList(wrapper).stream()
                .map(relation -> new SystemRoleMenuVO(relation.getMenuId(), relation.getAccessLevel()))
                .toList();
    }

    /**
     * replace
     *
     * @param roleId 参数 roleId
     * @param menus 参数 menus
     * @return 处理结果
     */
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

    /**
     * 移除 By Role Id
     *
     * @param roleId 参数 roleId
     */
    @Override
    public void removeByRoleId(Long roleId) {
        LambdaQueryWrapper<SystemRoleMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemRoleMenuRelEntity::getRoleId, roleId);
        roleMenuRelMapper.delete(wrapper);
    }

    /**
     * 移除 By Menu Ids
     *
     * @param menuIds 参数 menuIds
     */
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

    /**
     * 移除 By Tenant And Menu Ids
     *
     * @param tenantId 参数 tenantId
     * @param menuIds 参数 menuIds
     */
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
