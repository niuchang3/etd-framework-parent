package org.etd.framework.starter.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 框架 HTTP 请求拦截器基类
 * 负责全局 RequestContext 上下文的生命周期管理与异常安全的 ThreadLocal 清理
 *
 * @author Young
 * @date 2020/11/12
 */
public abstract class EtdFrameworkHttpRequestInterceptor implements HandlerInterceptor {

    /**
     * 上下文初始化就绪后执行的业务逻辑钩子（如租户校验、权限拦截等）
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 是否放行请求
     * @throws Exception 业务处理异常
     */
    /**
     * do 处理
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param handler 参数 handler
     * @return 处理结果
     */
    protected abstract boolean doHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

    /**
     * pre 处理
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param handler 参数 handler
     * @return 处理结果
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 1. 优先完成 RequestContext 线程上下文的解析与初始化
            RequestContextInitializer.init(request);

            // 2. 执行子类业务钩子逻辑
            return doHandle(request, response, handler);
        } catch (Exception ex) {
            // 关键安全防线：当 preHandle 抛出异常中断时，Spring MVC 不会触发 afterCompletion，
            // 必须在此处立即强行清理 ThreadLocal，防止线程池复用造成严重的跨租户污染与内存泄露！
            RequestContext.clean();
            throw ex;
        }
    }

    /**
     * 请求完成后清理线程上下文内容
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        RequestContext.clean();
    }

    /**
     * 获取拦截器路由路径
     *
     * @return 拦截路径列表
     */
    public abstract List<String> getInterceptorsPath();
}
