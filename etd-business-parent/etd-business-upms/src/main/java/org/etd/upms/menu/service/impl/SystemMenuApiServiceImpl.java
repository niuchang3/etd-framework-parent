package org.etd.upms.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.upms.menu.entity.SystemMenuApiRelEntity;
import org.etd.upms.menu.mapper.SystemMenuApiRelMapper;
import org.etd.upms.menu.service.SystemMenuApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class SystemMenuApiServiceImpl implements SystemMenuApiService {

    @Autowired
    private SystemMenuApiRelMapper menuApiRelMapper;

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
