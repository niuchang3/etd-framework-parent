package org.etd.upms.service.menu.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.etd.upms.converter.SystemMenusConverter;
import org.etd.upms.entity.SystemMenusEntity;
import org.etd.upms.mapper.menu.SystemMenusMapper;
import org.etd.upms.service.menu.SystemMenusService;
import org.etd.upms.controller.user.vo.SystemUserMenusVO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemMenusServiceImpl implements SystemMenusService {


    @Autowired
    private SystemMenusMapper systemMenusMapper;

    /**
     * 菜单过滤
     *
     * @param menuIds
     * @return
     */
    @Override
    public List<SystemUserMenusVO> filterMenu(String menuIds, Long tenantId) {
        List<SystemUserMenusVO> systemAllMenus = getSystemAllMenus();
        return filterMenu(systemAllMenus,menuIds,tenantId);
    }


    @Override
    public List<SystemUserMenusVO> filterMenu(List<SystemUserMenusVO> systemAllMenus,String menuIds, Long tenantId) {
        List<Long> tenantMenuIds = Arrays.stream(menuIds.split(","))
                .map(strValue -> Long.valueOf(strValue))
                .collect(Collectors.toList());
        List<SystemUserMenusVO> collect = systemAllMenus.stream()
                .filter(item -> tenantMenuIds.contains(item.getId()))
                .collect(Collectors.toList());

        collect.stream().forEach(item -> item.setTenantId(tenantId));
        return collect;
    }

    /**
     * 获取普通用户
     *
     * @return
     */
    private List<SystemUserMenusVO> getSystemAllMenus() {
        List<SystemMenusEntity> systemMenus = systemMenusMapper.selectList(new QueryWrapper<>());
        return Mappers.getMapper(SystemMenusConverter.class).toUserMenu(systemMenus);
    }


}
