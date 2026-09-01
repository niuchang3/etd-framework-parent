package org.etd.upms.tenant.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.upms.tenant.biz.SystemTenantBizService;
import org.etd.upms.tenant.controller.dto.SystemTenantCreateDTO;
import org.etd.upms.tenant.controller.dto.SystemTenantUpdateDTO;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/v1/tenant")
public class SystemTenantController {

    @Autowired
    private SystemTenantBizService tenantBizService;



    @GetMapping
    public ResultModel page(@RequestParam("current") Long current,
                            @RequestParam("size") Long size,
                            @RequestParam(name = "times", required = false) List<String> times,
                            @RequestParam(name = "keyword", required = false) String keyword){
        IPage<SystemTenantVO> page = tenantBizService.page(new Page<>(current, size), times, keyword);
        return ResultModel.success(page);
    }

    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemTenantCreateDTO dto) {
        return ResultModel.success(tenantBizService.insert(dto));
    }

    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody SystemTenantUpdateDTO dto) {
        return ResultModel.success(tenantBizService.update(id, dto));
    }

    @PatchMapping("/{id}/status/{status}")
    public ResultModel<Boolean> switchStatus(
            @PathVariable("id") Long id,
            @PathVariable("status") @Min(value = BasicConstant.DataStatus.DISABLED_CODE,
                    message = "租户状态只能为0或1")
            @Max(value = BasicConstant.DataStatus.ENABLED_CODE, message = "租户状态只能为0或1") Integer status) {
        return ResultModel.success(tenantBizService.switchStatus(id, status));
    }

    @PatchMapping("/{id}/locked/{locked}")
    public ResultModel<Boolean> switchLocked(@PathVariable("id") Long id,
                                             @PathVariable("locked") Boolean locked) {
        return ResultModel.success(tenantBizService.switchLocked(id, locked));
    }

    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable("id") Long id) {
        return ResultModel.success(tenantBizService.delete(id));
    }
}
