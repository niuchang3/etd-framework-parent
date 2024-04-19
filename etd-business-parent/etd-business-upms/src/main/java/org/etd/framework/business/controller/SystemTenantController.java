package org.etd.framework.business.controller;


import org.etd.framework.business.service.SystemTenantService;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tenant")
public class SystemTenantController {

    @Autowired
    private SystemTenantService tenantService;


    @PostMapping
    public ResultModel<Boolean> insert(){
        return ResultModel.success(true);
    }
}
