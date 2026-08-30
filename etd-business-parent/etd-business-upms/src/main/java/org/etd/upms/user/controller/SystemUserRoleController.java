package org.etd.upms.user.controller;


import org.etd.upms.user.service.SystemUserRoleRelService;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/user/role")
public class SystemUserRoleController {

    @Autowired
    private SystemUserRoleRelService userRoleRelService;



    @PostMapping
    public ResultModel<Boolean> insert(){
        return ResultModel.success(true);
    }
}
