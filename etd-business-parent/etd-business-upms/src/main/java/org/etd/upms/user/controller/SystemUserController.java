package org.etd.upms.user.controller;


import org.etd.framework.common.core.user.UserDetails;
import org.etd.upms.tenant.biz.SystemTenantBizService;
import org.etd.upms.user.biz.SystemUserBizService;
import org.etd.upms.user.converter.SystemUserConverter;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.controller.vo.SystemUserVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/v1/user")
public class SystemUserController {

    @Autowired
    private SystemTenantBizService tenantBizService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemUserBizService userBizService;

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
        List<SystemTenantVO> tenantVOS = tenantBizService.selectByUser(RequestContext.getUser());
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
        List<SystemUserMenusVO> systemUserMenusVOS = userBizService.currentUserMenus();
        return ResultModel.success(systemUserMenusVOS);
    }


}
