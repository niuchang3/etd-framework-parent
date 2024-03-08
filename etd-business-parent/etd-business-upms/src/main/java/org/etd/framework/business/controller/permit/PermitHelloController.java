package org.etd.framework.business.controller.permit;

import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */
@Slf4j
@RestController
@RequestMapping("/permit/hello")
public class PermitHelloController {


    @AutoLog("Hello Controller")
    @PreAuthorize("hasAnyAuthority('TEST_1') || hasAnyRole('ADMIN')")
    @GetMapping
    public ResultModel hello() {
        return ResultModel.success("需要认证的接口");
    }

}
