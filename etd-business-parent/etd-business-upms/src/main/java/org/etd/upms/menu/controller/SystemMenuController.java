package org.etd.upms.menu.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.upms.menu.biz.SystemMenuBizService;
import org.etd.upms.menu.controller.dto.SystemMenuSaveDTO;
import org.etd.upms.menu.controller.vo.SystemMenuVO;
import org.etd.upms.menu.service.SystemMenusService;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/menu")
public class SystemMenuController {

    @Autowired
    private SystemMenusService menusService;

    @Autowired
    private SystemMenuBizService menuBizService;

    @GetMapping("/{id}")
    public ResultModel<SystemMenuVO> detail(@PathVariable Long id) {
        return ResultModel.success(menusService.selectById(id));
    }

    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemMenuSaveDTO dto) {
        return ResultModel.success(menuBizService.insert(dto));
    }

    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id,
                                       @Valid @RequestBody SystemMenuSaveDTO dto) {
        return ResultModel.success(menusService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable Long id) {
        return ResultModel.success(menuBizService.delete(id));
    }

    @PatchMapping("/{id}/status/{status}")
    public ResultModel<Boolean> switchStatus(
            @PathVariable Long id,
            @PathVariable @Min(value = BasicConstant.DataStatus.DISABLED_CODE, message = "菜单状态只能为0或1")
            @Max(value = BasicConstant.DataStatus.ENABLED_CODE, message = "菜单状态只能为0或1") Integer status) {
        return ResultModel.success(menusService.switchStatus(id, status));
    }
}
