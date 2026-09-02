package org.etd.upms.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.etd.upms.menu.entity.SystemMenuApiRelEntity;
import org.etd.upms.menu.mapper.SystemMenuApiRelMapper;
import org.etd.upms.menu.service.SystemMenuApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 菜单与 API 接口权限关联关系 Service 实现类。
 */
@Service
public class SystemMenuApiServiceImpl implements SystemMenuApiService {

    @Autowired
    private SystemMenuApiRelMapper menuApiRelMapper;

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
        LambdaQueryWrapper<SystemMenuApiRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemMenuApiRelEntity::getMenuId, menuIds);
        menuApiRelMapper.delete(wrapper);
    }
}
