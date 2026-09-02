package org.etd.upms.tenant.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.upms.tenant.biz.SystemTenantBizService;
import org.etd.upms.tenant.controller.dto.SystemTenantCreateDTO;
import org.etd.upms.tenant.controller.dto.SystemTenantMenuAssignDTO;
import org.etd.upms.tenant.controller.dto.SystemTenantUpdateDTO;
import org.etd.upms.tenant.controller.vo.SystemTenantMenuSettingsVO;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.Instant;

/**
 * 租户管理 Controller 控制器入口。
 */
@RestController
@Validated
@RequestMapping("/v1/tenant")
public class SystemTenantController {

    @Autowired
    private SystemTenantBizService tenantBizService;



    /**
     * 分页查询
     *
     * @param @RequestParam("current" 参数 @RequestParam("current"
     * @return 处理结果
     */
    @GetMapping
    public ResultModel page(@RequestParam("current") Long current,
                            @RequestParam("size") Long size,
                            @RequestParam(name = "times", required = false) List<Instant> times,
                            @RequestParam(name = "keyword", required = false) String keyword){
        IPage<SystemTenantVO> page = tenantBizService.page(new Page<>(current, size), times, keyword);
        return ResultModel.success(page);
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemTenantCreateDTO dto) {
        return ResultModel.success(tenantBizService.insert(dto));
    }

    /**
     * 更新修改
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody SystemTenantUpdateDTO dto) {
        return ResultModel.success(tenantBizService.update(id, dto));
    }

    /**
     * menus
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @GetMapping("/{id}/menus")
    public ResultModel<SystemTenantMenuSettingsVO> menus(@PathVariable("id") Long id) {
        return ResultModel.success(tenantBizService.selectMenuSettings(id));
    }

    /**
     * replace Menus
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @PutMapping("/{id}/menus")
    public ResultModel<Boolean> replaceMenus(@PathVariable("id") Long id,
                                             @Valid @RequestBody SystemTenantMenuAssignDTO dto) {
        return ResultModel.success(tenantBizService.replaceMenus(id, dto.getMenuIds()));
    }

    @PatchMapping("/{id}/status/{status}")
    public ResultModel<Boolean> switchStatus(
            @PathVariable("id") Long id,
            @PathVariable("status") @Min(value = BasicConstant.DataStatus.DISABLED_CODE,
                    message = "租户状态只能为0或1")
            @Max(value = BasicConstant.DataStatus.ENABLED_CODE, message = "租户状态只能为0或1") Integer status) {
        return ResultModel.success(tenantBizService.switchStatus(id, status));
    }

    /**
     * 切换 Locked
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @PatchMapping("/{id}/locked/{locked}")
    public ResultModel<Boolean> switchLocked(@PathVariable("id") Long id,
                                             @PathVariable("locked") Boolean locked) {
        return ResultModel.success(tenantBizService.switchLocked(id, locked));
    }

    /**
     * 删除
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable("id") Long id) {
        return ResultModel.success(tenantBizService.delete(id));
    }
}
