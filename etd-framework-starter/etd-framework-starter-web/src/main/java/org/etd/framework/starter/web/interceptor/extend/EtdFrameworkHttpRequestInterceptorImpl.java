package org.etd.framework.starter.web.interceptor.extend;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.etd.framework.common.core.user.UserDetails;
import com.google.common.collect.Lists;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */
public class EtdFrameworkHttpRequestInterceptorImpl extends EtdFrameworkHttpRequestInterceptor {

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

        for (RequestMatcher matcher : whiteList) {
            if(matcher.matches(request)){
                return;
            }
        }

        UserDetails user = RequestContext.getUser();
        if (ObjectUtils.isEmpty(user)) {
            return;
        }
        if (ObjectUtils.isEmpty(user.getTenantId())) {
            throw new ApiRuntimeException("登录用户未绑定租户。");
        }
        Long tenantCode = RequestContext.getTenantCode();
        if (ObjectUtils.isEmpty(tenantCode)) {
            RequestContext.setTenantCode(user.getTenantId());
            return;
        }
        if (!user.getTenantId().equals(tenantCode)) {
            throw new ApiRuntimeException("用户无权切换到其他租户。");
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
