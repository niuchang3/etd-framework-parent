package org.etd.framework.business.controller;


import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/v1/user")
public class UserController {

    @GetMapping(value = "/me")
    public ResultModel<UserDetails> me() {
        UserDetails user = RequestContext.getUser();
        return ResultModel.success(user);
    }
}
