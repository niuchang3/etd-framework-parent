package org.etd.upms.role.controller;

import com.etd.framework.starter.client.core.permission.annotation.Permission;
import org.etd.upms.menu.constant.MenuPermissionCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.upms.role.biz.SystemRoleBizService;
import org.etd.upms.role.controller.dto.SystemRoleMenuAssignDTO;
import org.etd.upms.role.controller.dto.SystemRoleOrganizationAssignDTO;
import org.etd.upms.role.controller.dto.SystemRoleSaveDTO;
import org.etd.upms.role.controller.vo.SystemRoleMenuVO;
import org.etd.upms.role.controller.vo.SystemRoleVO;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.role.service.SystemRoleService;
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
import java.util.Set;

/**
 * 系统角色管理 Controller 控制器入口。
 */
@Validated
@Permission(MenuPermissionCode.ROLE)
@RestController
@RequestMapping("/v1/role")
public class SystemRoleController {

    @Autowired
    private SystemRoleService roleService;

    @Autowired
    private SystemRoleMenuService roleMenuService;

    @Autowired
    private SystemRoleBizService roleBizService;

    @AutoLog("分页查询系统角色列表")
    @GetMapping
    public ResultModel<IPage<SystemRoleVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 200, message = "每页条数不能超过200") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(value = BasicConstant.DataStatus.DISABLED_CODE)
            @Max(value = BasicConstant.DataStatus.ENABLED_CODE) Integer dataStatus,
            @RequestParam(required = false) Boolean assignableOnly) {
        return ResultModel.success(roleService.page(current, size, keyword, dataStatus, assignableOnly));
    }

    /**
     * detail
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @AutoLog("获取系统角色详情")
    @GetMapping("/{id}")
    public ResultModel<SystemRoleVO> detail(@PathVariable Long id) {
        return ResultModel.success(roleService.selectById(id));
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @AutoLog("新增系统角色")
    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemRoleSaveDTO dto) {
        return ResultModel.success(roleBizService.insert(dto));
    }

    /**
     * 更新修改
     *
     * @param id 参数 id
     * @param dto 参数 dto
     * @return 处理结果
     */
    @AutoLog("更新系统角色")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody SystemRoleSaveDTO dto) {
        return ResultModel.success(roleBizService.update(id, dto));
    }

    /**
     * 删除
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @AutoLog("删除系统角色")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable Long id) {
        return ResultModel.success(roleBizService.delete(id));
    }

    @AutoLog("切换系统角色状态")
    @PatchMapping("/{id}/status/{status}")
    public ResultModel<Boolean> switchStatus(
            @PathVariable Long id,
            @PathVariable @Min(value = BasicConstant.DataStatus.DISABLED_CODE, message = "角色状态只能为0或1")
            @Max(value = BasicConstant.DataStatus.ENABLED_CODE, message = "角色状态只能为0或1") Integer status) {
        return ResultModel.success(roleService.switchStatus(id, status));
    }

    /**
     * menus
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @AutoLog("获取角色关联菜单")
    @GetMapping("/{id}/menus")
    public ResultModel<List<SystemRoleMenuVO>> menus(@PathVariable Long id) {
        roleService.requireExists(id);
        return ResultModel.success(roleMenuService.selectByRoleId(id));
    }

    @AutoLog("分配角色菜单权限")
    @PutMapping("/{id}/menus")
    public ResultModel<Boolean> replaceMenus(@PathVariable Long id,
                                               @Valid @RequestBody SystemRoleMenuAssignDTO dto) {
        return ResultModel.success(roleBizService.replaceMenus(id, dto.getMenus()));
    }

    /**
     * 根据角色 ID 获取关联的组织机构 ID 集合
     */
    @AutoLog("获取角色关联组织机构ID集合")
    @GetMapping("/{id}/organizations")
    public ResultModel<Set<Long>> getOrganizationIds(@PathVariable Long id) {
        return ResultModel.success(roleBizService.selectOrganizationIds(id));
    }

    @AutoLog("分配角色数据权限")
    @PutMapping("/{id}/organizations")
    public ResultModel<Boolean> replaceOrganizations(
            @PathVariable Long id, @Valid @RequestBody SystemRoleOrganizationAssignDTO dto) {
        return ResultModel.success(roleBizService.replaceOrganizations(id, dto.getOrganizationIds()));
    }
}
