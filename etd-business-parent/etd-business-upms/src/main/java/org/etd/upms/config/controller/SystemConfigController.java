package org.etd.upms.config.controller;

import com.etd.framework.starter.client.core.permission.annotation.Permission;
import org.etd.upms.menu.constant.MenuPermissionCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.upms.config.controller.dto.SystemConfigSaveDTO;
import org.etd.upms.config.controller.vo.SystemConfigVO;
import org.etd.upms.config.service.SystemConfigService;
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


/**
 * 系统参数配置 Controller 控制器入口。
 */
@Validated
@Permission(MenuPermissionCode.CONFIG)
@RestController
@RequestMapping("/v1/config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService configService;

    @GetMapping
    public ResultModel<IPage<SystemConfigVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 200, message = "每页条数不能超过200") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String valueType) {
        return ResultModel.success(configService.page(current, size, keyword, enabled, valueType));
    }

    /**
     * detail
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @GetMapping("/{id}")
    public ResultModel<SystemConfigVO> detail(@PathVariable Long id) {
        return ResultModel.success(configService.selectById(id));
    }

    @GetMapping("/key/{parameterKey}")
    public ResultModel<SystemConfigVO> enabledByKey(
            @PathVariable @NotBlank(message = "参数键不能为空") String parameterKey) {
        return ResultModel.success(configService.selectEnabledByKey(parameterKey));
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemConfigSaveDTO dto) {
        return ResultModel.success(configService.insert(dto));
    }

    /**
     * 更新修改
     *
     * @param id 参数 id
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody SystemConfigSaveDTO dto) {
        return ResultModel.success(configService.update(id, dto));
    }

    /**
     * 删除
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable Long id) {
        return ResultModel.success(configService.delete(id));
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
        return ResultModel.success(configService.switchEnabled(id, enabled));
    }
}
