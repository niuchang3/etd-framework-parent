package org.etd.framework.starter.web.interceptor.extend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认 HTTP 请求处理拦截器实现（包含租户校验与白名单过滤）
 *
 * @author Young
 * @date 2020/11/12
 */
public class EtdFrameworkHttpRequestInterceptorImpl extends EtdFrameworkHttpRequestInterceptor {

    private final List<String> whiteList = new ArrayList<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public EtdFrameworkHttpRequestInterceptorImpl addWhiteList(String pattern) {
        if (pattern != null) {
            whiteList.add(pattern);
        }
        return this;
    }

    @Override
    protected boolean doHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }

        UserDetails user = RequestContext.getUser();
        if (ObjectUtils.isEmpty(user)) {
            return true;
        }
        if (ObjectUtils.isEmpty(user.getTenantId())) {
            throw new ApiRuntimeException("登录用户未绑定租户。");
        }
        Long tenantCode = RequestContext.getTenantCode();
        if (ObjectUtils.isEmpty(tenantCode)) {
            RequestContext.setTenantCode(user.getTenantId());
            return true;
        }
        if (!user.getTenantId().equals(tenantCode)) {
            throw new ApiRuntimeException("用户无权切换到其他租户。");
        }

        return true;
    }

    @Override
    public List<String> getInterceptorsPath() {
        List<String> list = new ArrayList<>();
        list.add("/**");
        return list;
    }
}
