package org.etd.framework.starter.web.interceptor.extend;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.etd.framework.starter.client.core.TenantAuthority;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.utils.request.RequestUtil;
import org.etd.framework.starter.web.interceptor.CustomInterceptor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */
public class CustomDefaultInterceptor extends CustomInterceptor {

    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    @Override
    protected void beforeHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

    }

    @Override
    protected void afterHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        SecurityContext context = SecurityContextHolder.getContext();
        RequestContext.setRequestIP(RequestUtil.getRemoteIp(request));
        String tenantCode =  request.getHeader(BasicConstant.MessageHeader.tenant_id.name());
        RequestContext.setTenantCode(Long.valueOf(tenantCode));
        RequestContext.setTraceId(snowflake.nextIdStr());

        if (ObjectUtils.isEmpty(context.getAuthentication())) {
            return;
        }
        if (ObjectUtils.isEmpty(context.getAuthentication().getDetails())) {
            return;
        }
        if (context.getAuthentication().getDetails() instanceof UserDetails) {
            UserDetails details = (UserDetails) context.getAuthentication().getDetails();
            RequestContext.setUser(details);
            RequestContext.setToken((String) context.getAuthentication().getCredentials());


            List<TenantAuthority> authorities = details.getAuthorities();
            for (TenantAuthority authority : authorities) {
                if(BasicConstant.SystemRole.PlatformAdmin.name().equals(authority.getRoleCode())){
                    details.setPlatformAdmin(true);
                    return;
                }
            }
            Map<Long, List<TenantAuthority>> tenantAuthorityMap = authorities.stream().collect(Collectors.groupingBy(TenantAuthority::getTenantId));
            List<TenantAuthority> tenantAuthority = tenantAuthorityMap.get(RequestContext.getTenantCode());

            for (TenantAuthority authority : tenantAuthority) {
                if(BasicConstant.SystemRole.TenantAdmin.name().equals(authority.getRoleCode())){
                    details.setTenantAdmin(true);
                    return;
                }
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
