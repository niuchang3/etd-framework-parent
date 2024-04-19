package org.etd.framework.business.controller;


import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.common.collect.Lists;
import org.etd.framework.business.converter.SystemUserConverter;
import org.etd.framework.business.service.SystemMenusService;
import org.etd.framework.business.service.SystemTenantService;
import org.etd.framework.business.service.SystemUserRoleRelService;
import org.etd.framework.business.vo.SystemTenantVO;
import org.etd.framework.business.vo.SystemUserMenusVO;
import org.etd.framework.business.vo.SystemUserRoleVO;
import org.etd.framework.business.vo.SystemUserVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/v1/user")
public class SystemUserController {

    @Autowired
    private SystemTenantService tenantService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemMenusService systemMenusService;

    /**
     * 获取当前登录人个人信息
     *
     * @return
     */
    @IgnoreTenant
    @GetMapping(value = "/me")
    public ResultModel<SystemUserVO> me() {
        UserDetails user = RequestContext.getUser();
        SystemUserVO systemUserVO = Mappers.getMapper(SystemUserConverter.class).toUserVO(user);
        return ResultModel.success(systemUserVO);
    }

    /**
     * 获取当前登录人租户信息
     *
     * @return
     */
    @IgnoreTenant
    @GetMapping("/tenant")
    public ResultModel<List<SystemTenantVO>> currentUserTenant() {
        List<SystemTenantVO> tenantVOS = tenantService.selectByUser(RequestContext.getUser());
        return ResultModel.success(tenantVOS);
    }

    /**
     * 获取当前登录人角色信息
     *
     * @return
     */
    @GetMapping("/role")
    public ResultModel<List<SystemUserRoleVO>> currentUserRole() {
        UserDetails user = RequestContext.getUser();
        List<SystemUserRoleVO> systemUserRoleVOS = userRoleRelService.selectByUser(user.getId());
        return ResultModel.success(systemUserRoleVOS);
    }


    /**
     * 获取当前登录人菜单权限
     *
     * @return
     */
    @GetMapping("/menus")
    public ResultModel<List<SystemUserMenusVO>> currentUserMenus() {
        List<SystemUserMenusVO> systemUserMenusVOS = systemMenusService.currentUserMenu();
        return ResultModel.success(systemUserMenusVOS);
    }


}
