package org.etd.framework.business.controller;


import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.common.collect.Lists;
import org.etd.framework.business.vo.Authority;
import org.etd.framework.business.vo.Tenant;
import org.etd.framework.business.vo.UserVO;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/v1/user")
public class UserController {

    @GetMapping(value = "/me")
    public ResultModel<UserVO> me() {
        UserDetails user = RequestContext.getUser();
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user,vo);

        if(CollectionUtils.isEmpty(user.getAuthorities())){
            return ResultModel.success(vo);
        }
        Map<Long, List<TenantAuthority>> tenantMap = user.getAuthorities().stream().collect(Collectors.groupingBy(TenantAuthority::getTenantId));

        List<Tenant> tenants  = Lists.newArrayList();

        tenantMap.forEach((key,item) ->{
            Tenant tenant = new Tenant();
            tenant.setId(key);
            tenant.setTenantName(item.get(0).getTenantName());
            List<Authority> objects = Lists.newArrayList();
            for (TenantAuthority authority : item) {
                Authority userAuthority = new Authority();
                BeanUtils.copyProperties(authority,userAuthority);
                objects.add(userAuthority);
            }
            tenant.setAuthorities(objects);
            tenants.add(tenant);
        });
        vo.setTenant(tenants);
        return ResultModel.success(vo);
    }
}
