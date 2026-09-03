package org.etd.upms.user.controller;


import com.etd.framework.starter.client.core.permission.annotation.Permission;
import org.etd.upms.menu.constant.MenuPermissionCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.etd.upms.user.biz.SystemUserBizService;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.controller.vo.SystemUserVO;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;
import org.etd.upms.user.controller.dto.SystemUserCreateDTO;
import org.etd.upms.user.controller.dto.SystemUserOrganizationAssignDTO;
import org.etd.upms.user.controller.dto.SystemUserRoleAssignDTO;
import org.etd.upms.user.controller.dto.SystemUserUpdateDTO;
import org.etd.framework.common.core.model.ResultModel;
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



/** 用户管理入口，统一校验用户资源的读写权限。 */
@Validated
@Permission(MenuPermissionCode.USER)
@RestController
@RequestMapping("/v1/user")
public class SystemUserController {

    @Autowired
    private SystemUserBizService userBizService;

    /**
     * 用户分页。按主组织及其下级筛选，并始终受当前登录人的数据权限限制。
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

    /**
     * detail
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @GetMapping("/{id}")
    public ResultModel<SystemUserVO> detail(@PathVariable Long id) {
        return ResultModel.success(userBizService.detail(id));
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemUserCreateDTO dto) {
        return ResultModel.success(userBizService.insert(dto));
    }

    /**
     * 更新修改
     *
     * @param id 参数 id
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody SystemUserUpdateDTO dto) {
        return ResultModel.success(userBizService.update(id, dto));
    }

    /**
     * 删除
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable Long id) {
        return ResultModel.success(userBizService.delete(id));
    }

    /**
     * 切换 Enabled
     *
     * @param id 参数 id
     * @param enabled 参数 enabled
     * @return 处理结果
     */
    @PatchMapping("/{id}/enabled/{enabled}")
    public ResultModel<Boolean> switchEnabled(@PathVariable Long id, @PathVariable Boolean enabled) {
        return ResultModel.success(userBizService.switchEnabled(id, enabled));
    }

    /**
     * 切换 Locked
     *
     * @param id 参数 id
     * @param locked 参数 locked
     * @return 处理结果
     */
    @PatchMapping("/{id}/locked/{locked}")
    public ResultModel<Boolean> switchLocked(@PathVariable Long id, @PathVariable Boolean locked) {
        return ResultModel.success(userBizService.switchLocked(id, locked));
    }

    /**
     * roles
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @GetMapping("/{id}/roles")
    public ResultModel<List<SystemUserRoleVO>> roles(@PathVariable Long id) {
        return ResultModel.success(userBizService.selectRoles(id));
    }

    @PutMapping("/{id}/roles")
    public ResultModel<Boolean> replaceRoles(@PathVariable Long id,
                                             @Valid @RequestBody SystemUserRoleAssignDTO dto) {
        return ResultModel.success(userBizService.replaceRoles(id, dto));
    }

    /**
     * 根据用户 ID 获取关联组织列表
     */
    @GetMapping("/{id}/organizations")
    public ResultModel<List<SystemUserOrganizationVO>> getOrganizationList(@PathVariable Long id) {
        return ResultModel.success(userBizService.selectOrganizationListByUser(id));
    }

    @PutMapping("/{id}/organizations")
    public ResultModel<Boolean> replaceOrganizations(
            @PathVariable Long id, @Valid @RequestBody SystemUserOrganizationAssignDTO dto) {
        return ResultModel.success(userBizService.replaceOrganizations(id, dto));
    }

}
