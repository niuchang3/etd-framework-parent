package org.etd.framework.business.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.business.service.SystemTenantService;
import org.etd.framework.business.vo.SystemTenantVO;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenant")
public class SystemTenantController {

    @Autowired
    private SystemTenantService tenantService;


    @PostMapping
    public ResultModel<Boolean> insert(){
        return ResultModel.success(true);
    }

    @GetMapping
    public ResultModel page(@RequestParam Long current,
                            @RequestParam Long size,
                            @RequestParam(required = false) List<String> times,
                            @RequestParam(required = false) String keyword){
        IPage<SystemTenantVO> page = tenantService.page(new Page(current, size),times , keyword);
        return ResultModel.success(page);
    }
}
