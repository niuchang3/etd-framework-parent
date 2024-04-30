package org.etd.framework.starter.web.interceptor;

import org.etd.framework.common.core.context.extend.HttpServletRequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Young
 * @description 自定义拦截器接口
 * @date 2020/11/12
 */

public abstract class EtdFrameworkHttpRequestInterceptor extends HttpServletRequestContextInitializer implements HandlerInterceptor {


    /**
     * 执行 preHandle之前的钩子函数
     *
     * @param request
     * @param response
     * @param handler
     */
    protected abstract void beforeHandle(HttpServletRequest request, HttpServletResponse response, Object handler);

    /**
     * 执行 preHandle之后的钩子函数
     *
     * @param request
     * @param response
     * @param handler
     */
    protected abstract void afterHandle(HttpServletRequest request, HttpServletResponse response, Object handler);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        beforeHandle(request, response, handler);
        initialization(request);
        afterHandle(request, response, handler);
        return true;
    }

    /**
     * 清空请求上下文内容
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        RequestContext.clean();
    }


    /**
     * 获取拦截器路径
     *
     * @return
     */
    public abstract List<String> getInterceptorsPath();
}
