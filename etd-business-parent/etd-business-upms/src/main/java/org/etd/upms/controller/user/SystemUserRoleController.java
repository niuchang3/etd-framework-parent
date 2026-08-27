package org.etd.upms.controller.user;


import org.etd.framework.common.core.user.TenantAuthority;
import org.etd.upms.converter.SystemUserConverter;
import org.etd.upms.service.user.SystemUserRoleRelService;
import org.etd.upms.controller.user.vo.SystemUserVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.ResultModel;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
