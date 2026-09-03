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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


import org.etd.upms.organization.biz.SystemOrganizationBizService;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;

/** 当前登录用户的个人信息和导航入口，仅要求登录，不继承管理权限。 */
@Validated
@RestController
@RequestMapping("/v1/user")
public class CurrentUserController {

    @Autowired
    private SystemTenantBizService tenantBizService;
    @Autowired
    private SystemUserRoleRelService userRoleRelService;
    @Autowired
    private SystemUserBizService userBizService;
    @Autowired
    private SystemOrganizationBizService organizationBizService;

    /**
     * 获取当前登录人个人信息
     */
    @IgnoreTenant
    @GetMapping(value = "/me")
    public ResultModel<SystemUserVO> me() {
        UserDetails user = RequestContext.getUser();
        SystemUserVO systemUserVO = Mappers.getMapper(SystemUserConverter.class).toUserVO(user);
        return ResultModel.success(systemUserVO);
    }

    /**
     * 获取当前登录人租户列表
     */
    @IgnoreTenant
    @GetMapping("/tenant")
    public ResultModel<List<SystemTenantVO>> getCurrentUserTenantList() {
        List<SystemTenantVO> tenantVOS = tenantBizService.selectTenantListByUser(RequestContext.getUser());
        return ResultModel.success(tenantVOS);
    }

    /**
     * 获取当前登录人角色列表
     */
    @GetMapping("/role")
    public ResultModel<List<SystemUserRoleVO>> getCurrentUserRoleList() {
        UserDetails user = RequestContext.getUser();
        List<SystemUserRoleVO> systemUserRoleVOS = userRoleRelService.selectByUser(user.getId());
        return ResultModel.success(systemUserRoleVOS);
    }


    /**
     * 获取当前登录人菜单权限
     *
     * @return 处理结果
     */
    @GetMapping("/menus")
    public ResultModel<List<SystemUserMenusVO>> currentUserMenus() {
        List<SystemUserMenusVO> systemUserMenusVOS = userBizService.currentUserMenus();
        return ResultModel.success(systemUserMenusVOS);
    }

    /**
     * 获取当前登录人组织机构树列表
     */
    @GetMapping("/organization/tree")
    public ResultModel<List<SystemOrganizationVO>> currentUserOrganizationTree(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return ResultModel.success(organizationBizService.selectOrganizationTreeList(keyword, enabled));
    }

}
