package org.etd.framework.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.business.converter.SystemMenusConverter;
import org.etd.framework.business.entity.SystemMenusEntity;
import org.etd.framework.business.mapper.SystemMenusMapper;
import org.etd.framework.business.service.SystemMenusService;
import org.etd.framework.business.service.SystemTenantService;
import org.etd.framework.business.service.SystemUserRoleRelService;
import org.etd.framework.business.vo.SystemTenantVO;
import org.etd.framework.business.vo.SystemUserMenusVO;
import org.etd.framework.business.vo.SystemUserRoleVO;
import org.etd.framework.common.core.context.model.RequestContext;
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

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemTenantService systemTenantService;

    @Override
    public List<SystemUserMenusVO> currentUserMenu() {


        SystemTenantVO tenantVO = systemTenantService.selectCurrentTenant();
        //过滤出租户拥有的菜单项
        List<SystemUserMenusVO> systemUserMenusVOS = filterMenu(tenantVO.getMenus(), tenantVO.getId());

        UserDetails user = RequestContext.getUser();
        if(user.isPlatformAdmin() || user.isTenantAdmin()){
            return systemUserMenusVOS;
        }

        List<SystemUserRoleVO> UserRoleVOS = userRoleRelService.selectByUser(user.getId());
        StringBuffer mensStr = new StringBuffer();
        for (SystemUserRoleVO userRoleVO : UserRoleVOS) {
            mensStr.append(userRoleVO.getMenus()).append(",");
        }
        return filterMenu(systemUserMenusVOS,mensStr.toString(), UserRoleVOS.get(0).getTenantId());
    }


    /**
     * 菜单过滤
     *
     * @param menuIds
     * @return
     */
    private List<SystemUserMenusVO> filterMenu(String menuIds, Long tenantId) {
        List<SystemUserMenusVO> systemAllMenus = getSystemAllMenus();
        return filterMenu(systemAllMenus,menuIds,tenantId);
    }


    private List<SystemUserMenusVO> filterMenu(List<SystemUserMenusVO> systemAllMenus,String menuIds, Long tenantId) {
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
