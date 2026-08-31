package org.etd.upms.role.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.upms.role.biz.SystemRoleBizService;
import org.etd.upms.role.controller.dto.SystemRoleMenuAssignDTO;
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

@Validated
@RestController
@RequestMapping("/v1/role")
public class SystemRoleController {

    @Autowired
    private SystemRoleService roleService;

    @Autowired
    private SystemRoleMenuService roleMenuService;

    @Autowired
    private SystemRoleBizService roleBizService;

    @GetMapping
    public ResultModel<IPage<SystemRoleVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 200, message = "每页条数不能超过200") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(value = BasicConstant.DataStatus.DISABLED_CODE)
            @Max(value = BasicConstant.DataStatus.ENABLED_CODE) Integer dataStatus) {
        return ResultModel.success(roleService.page(current, size, keyword, dataStatus));
    }

    @GetMapping("/{id}")
    public ResultModel<SystemRoleVO> detail(@PathVariable Long id) {
        return ResultModel.success(roleService.selectById(id));
    }

    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemRoleSaveDTO dto) {
        return ResultModel.success(roleService.insert(dto));
    }

    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody SystemRoleSaveDTO dto) {
        return ResultModel.success(roleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable Long id) {
        return ResultModel.success(roleBizService.delete(id));
    }

    @PatchMapping("/{id}/status/{status}")
    public ResultModel<Boolean> switchStatus(
            @PathVariable Long id,
            @PathVariable @Min(value = BasicConstant.DataStatus.DISABLED_CODE, message = "角色状态只能为0或1")
            @Max(value = BasicConstant.DataStatus.ENABLED_CODE, message = "角色状态只能为0或1") Integer status) {
        return ResultModel.success(roleService.switchStatus(id, status));
    }

    @GetMapping("/{id}/menus")
    public ResultModel<List<SystemRoleMenuVO>> menus(@PathVariable Long id) {
        roleService.requireExists(id);
        return ResultModel.success(roleMenuService.selectByRoleId(id));
    }

    @PutMapping("/{id}/menus")
    public ResultModel<Boolean> replaceMenus(@PathVariable Long id,
                                              @Valid @RequestBody SystemRoleMenuAssignDTO dto) {
        return ResultModel.success(roleBizService.replaceMenus(id, dto.getMenus()));
    }
}
