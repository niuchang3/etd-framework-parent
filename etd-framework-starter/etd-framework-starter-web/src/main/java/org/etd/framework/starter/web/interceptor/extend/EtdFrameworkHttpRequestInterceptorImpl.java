package org.etd.framework.starter.web.interceptor.extend;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.common.collect.Lists;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */
public class EtdFrameworkHttpRequestInterceptorImpl extends EtdFrameworkHttpRequestInterceptor {

    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    private List<RequestMatcher> whiteList = Lists.newArrayList();

    public EtdFrameworkHttpRequestInterceptorImpl addWhiteList(RequestMatcher requestMatcher){
        whiteList.add(requestMatcher);
        return this;
    }


    @Override
    protected void beforeHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

    }


    @Override
    protected void afterHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        isPlatformAdmin();
        UserDetails user = RequestContext.getUser();
        if(user.isPlatformAdmin()){
            return;
        }

        for (RequestMatcher matcher : whiteList) {
            if(matcher.matches(request)){
                return;
            }
        }
        Long tenantCode = RequestContext.getTenantCode();
        if(ObjectUtils.isEmpty(RequestContext.getTenantCode())){
            throw new ApiRuntimeException("非法的租户信息。");
        }



        List<TenantAuthority> authorities = user.getAuthorities();
        if(ObjectUtils.isEmpty(authorities)){
            throw new ApiRuntimeException("该用户无权限访问,请联系管理员。");
        }

        Map<Long, List<TenantAuthority>> tenants = authorities.stream().collect(Collectors.groupingBy(TenantAuthority::getTenantId));
        if(!tenants.containsKey(tenantCode)){
            throw new ApiRuntimeException("该用户无权限访问该租户。");
        }



        List<TenantAuthority> tenantAuthorities = tenants.get(tenantCode);
        String roleCode = tenantAuthorities.get(0).getRoleCode();
        if(BasicConstant.SystemRole.TenantAdmin.name().equals(roleCode)){
            user.setPlatformAdmin(false);
            user.setTenantAdmin(true);
            RequestContext.setUser(user);
        }
    }

    private void isPlatformAdmin(){
        UserDetails user = RequestContext.getUser();
        if(ObjectUtils.isEmpty(user)){
            return;
        }
        List<TenantAuthority> authorities = user.getAuthorities();
        if(ObjectUtils.isEmpty(authorities)){
            return;
        }

        for (TenantAuthority authority : authorities) {
            if(BasicConstant.SystemRole.PlatformAdmin.name().equals(authority.getRoleCode())){
                user.setPlatformAdmin(true);
                user.setTenantAdmin(true);
                RequestContext.setUser(user);
                return;
            }
        }

    }

    /**
     * 获取拦截器路径
     *
     * @return
     */
    @Override
    public List<String> getInterceptorsPath() {
        List<String> list = new ArrayList<>();
        list.add("/**");
        return list;
    }
}
