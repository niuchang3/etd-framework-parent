package org.etd.upms.tenant.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.upms.tenant.biz.SystemTenantBizService;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenant")
public class SystemTenantController {

    @Autowired
    private SystemTenantService tenantService;

    @Autowired
    private SystemTenantBizService tenantBizService;



    @GetMapping
    public ResultModel page(@RequestParam("current") Long current,
                            @RequestParam("size") Long size,
                            @RequestParam(name = "times", required = false) List<String> times,
                            @RequestParam(name = "keyword", required = false) String keyword){
        IPage<SystemTenantVO> page = tenantBizService.page(new Page(current, size), times, keyword);
        return ResultModel.success(page);
    }

    @PatchMapping("/{id}/{status}")
    public ResultModel<Boolean> switchLocked(@PathVariable("id") Long id,
                                             @PathVariable("status") Boolean status){
        return ResultModel.success(tenantService.switchLocked(id,status));
    }
}
