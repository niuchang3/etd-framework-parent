package org.etd.upms.user.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.upms.tenant.biz.SystemTenantBizService;
import org.etd.upms.user.biz.SystemUserBizService;
import org.etd.upms.user.converter.SystemUserConverter;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.controller.vo.SystemUserVO;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;
import org.etd.upms.user.controller.dto.SystemUserCreateDTO;
import org.etd.upms.user.controller.dto.SystemUserOrganizationAssignDTO;
import org.etd.upms.user.controller.dto.SystemUserRoleAssignDTO;
import org.etd.upms.user.controller.dto.SystemUserUpdateDTO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Validated
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
     * 用户分页。组织参数为空时包含未分配组织的用户；传入时包含该组织及全部下级组织的用户。
     */
    @GetMapping
    public ResultModel<IPage<SystemUserVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 200, message = "每页条数不能超过200") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean locked) {
        return ResultModel.success(userBizService.page(current, size, keyword, organizationId, enabled, locked));
    }

    @GetMapping("/{id}")
    public ResultModel<SystemUserVO> detail(@PathVariable Long id) {
        return ResultModel.success(userBizService.detail(id));
    }

    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemUserCreateDTO dto) {
        return ResultModel.success(userBizService.insert(dto));
    }

    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody SystemUserUpdateDTO dto) {
        return ResultModel.success(userBizService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable Long id) {
        return ResultModel.success(userBizService.delete(id));
    }

    @PatchMapping("/{id}/enabled/{enabled}")
    public ResultModel<Boolean> switchEnabled(@PathVariable Long id, @PathVariable Boolean enabled) {
        return ResultModel.success(userBizService.switchEnabled(id, enabled));
    }

    @PatchMapping("/{id}/locked/{locked}")
    public ResultModel<Boolean> switchLocked(@PathVariable Long id, @PathVariable Boolean locked) {
        return ResultModel.success(userBizService.switchLocked(id, locked));
    }

    @GetMapping("/{id}/roles")
    public ResultModel<List<SystemUserRoleVO>> roles(@PathVariable Long id) {
        return ResultModel.success(userBizService.selectRoles(id));
    }

    @PutMapping("/{id}/roles")
    public ResultModel<Boolean> replaceRoles(@PathVariable Long id,
                                             @Valid @RequestBody SystemUserRoleAssignDTO dto) {
        return ResultModel.success(userBizService.replaceRoles(id, dto));
    }

    @GetMapping("/{id}/organizations")
    public ResultModel<List<SystemUserOrganizationVO>> organizations(@PathVariable Long id) {
        return ResultModel.success(userBizService.selectOrganizations(id));
    }

    @PutMapping("/{id}/organizations")
    public ResultModel<Boolean> replaceOrganizations(
            @PathVariable Long id, @Valid @RequestBody SystemUserOrganizationAssignDTO dto) {
        return ResultModel.success(userBizService.replaceOrganizations(id, dto));
    }

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
