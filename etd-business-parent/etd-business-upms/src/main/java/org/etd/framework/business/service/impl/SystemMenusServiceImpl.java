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
import org.etd.framework.business.vo.SystemUserRoleVO;
import org.etd.framework.business.vo.SystemUserMenusVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemMenusServiceImpl implements SystemMenusService {


    @Autowired
    private SystemMenusMapper systemMenusMapper;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemTenantService tenantService;


    @Override
    public List<SystemUserMenusVO> currentUserMenu() {
        UserDetails user = RequestContext.getUser();

        if (user.isPlatformAdmin()) {
            return getPlatformAdmin();
        }
        if(user.isTenantAdmin()){


        }



        List<SystemUserRoleVO> systemUserRoleVOS = userRoleRelService.selectUserRolesByTenant(user.getId());
//        systemUserRoleVOS.stream().map(SystemUserRoleVO::getTenantId)

//        user.getAuthorities().forEach();

        return null;
    }

    @IgnoreTenant
    public List<SystemUserMenusVO> getPlatformAdminMenus(){
        List<SystemMenusEntity> systemMenus = systemMenusMapper.selectList(new QueryWrapper<>());
        return Mappers.getMapper(SystemMenusConverter.class).toUserMenu(systemMenus);
    }

    public List<SystemUserMenusVO> getTenantAdminMenus(UserDetails user){
        List<SystemTenantVO> tenantVOS = tenantService.selectByUser(user.getId());
        SystemTenantVO systemTenantVO = tenantVOS.get(0);
        systemTenantVO.get
        return
    }

}
