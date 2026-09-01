package org.etd.upms.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.tenant.entity.SystemTenantMenuRelEntity;
import org.etd.upms.tenant.mapper.SystemTenantMenuRelMapper;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemTenantMenuServiceImpl implements SystemTenantMenuService {

    @Autowired
    private SystemTenantMenuRelMapper tenantMenuRelMapper;

    @IgnoreTenant
    @Override
    public Set<Long> selectMenuIds(Long tenantId) {
        LambdaQueryWrapper<SystemTenantMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTenantMenuRelEntity::getTenantId, tenantId)
                .eq(SystemTenantMenuRelEntity::getDataStatus, BasicConstant.DataStatus.ENABLED.getCode())
                .select(SystemTenantMenuRelEntity::getMenuId);
        return tenantMenuRelMapper.selectList(wrapper).stream()
                .map(SystemTenantMenuRelEntity::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean appendMenu(Long tenantId, Long menuId) {
        if (exists(tenantId, menuId)) {
            return true;
        }
        SystemTenantMenuRelEntity relation = new SystemTenantMenuRelEntity();
        relation.setTenantId(tenantId);
        relation.setMenuId(menuId);
        return tenantMenuRelMapper.insert(relation) > 0;
    }

    @IgnoreTenant
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean replace(Long tenantId, Set<Long> menuIds) {
        removeByTenantId(tenantId);
        for (Long menuId : menuIds) {
            SystemTenantMenuRelEntity relation = new SystemTenantMenuRelEntity();
            relation.setTenantId(tenantId);
            relation.setMenuId(menuId);
            relation.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
            if (tenantMenuRelMapper.insert(relation) <= 0) {
                throw new ApiRuntimeException("租户菜单权限保存失败。");
            }
        }
        return true;
    }

    @IgnoreTenant
    @Override
    public void removeByMenuIds(Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemTenantMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemTenantMenuRelEntity::getMenuId, menuIds);
        tenantMenuRelMapper.delete(wrapper);
    }

    private boolean exists(Long tenantId, Long menuId) {
        LambdaQueryWrapper<SystemTenantMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTenantMenuRelEntity::getTenantId, tenantId)
                .eq(SystemTenantMenuRelEntity::getMenuId, menuId);
        return tenantMenuRelMapper.selectCount(wrapper) > 0;
    }

    private void removeByTenantId(Long tenantId) {
        LambdaQueryWrapper<SystemTenantMenuRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemTenantMenuRelEntity::getTenantId, tenantId);
        tenantMenuRelMapper.delete(wrapper);
    }
}
