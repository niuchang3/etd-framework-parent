package org.etd.framework.business.controller;

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
@RequestMapping("/api")
public class HelloController {


    @AutoLog("Hello Controller")
    @PreAuthorize("hasAnyAuthority('TEST_1') || hasAnyRole('ADMIN')")
    @GetMapping("/permit")
    public ResultModel hello() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//		Object userDetail = UserDetailUtils.getUserDetail();
        return ResultModel.success("success");
    }


    //	@PreAuthorize("hasAnyAuthority('TEST_1') || hasAnyRole('ADMIN')")
    @AutoLog("Hello Controller")
    @GetMapping("/hello")
    public ResultModel helloController() {
        log.info("Hello Controller");
        return ResultModel.success("Hello Controller");
    }

}
