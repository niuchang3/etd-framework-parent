package org.etd.upms.controller.tenant;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.upms.biz.tenant.SystemTenantBizService;
import org.etd.upms.service.tenant.SystemTenantService;
import org.etd.upms.controller.tenant.vo.SystemTenantVO;
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
    public ResultModel page(@RequestParam Long current,
                            @RequestParam Long size,
                            @RequestParam(required = false) List<String> times,
                            @RequestParam(required = false) String keyword){
        IPage<SystemTenantVO> page = tenantBizService.page(new Page(current, size), times, keyword);
        return ResultModel.success(page);
    }

    @PatchMapping("/{id}/{status}")
    public ResultModel<Boolean> switchLocked(@PathVariable Long id,@PathVariable Boolean status){
        return ResultModel.success(tenantService.switchLocked(id,status));
    }
}
