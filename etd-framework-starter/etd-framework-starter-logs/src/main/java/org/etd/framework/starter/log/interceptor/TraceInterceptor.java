package org.etd.framework.starter.log.interceptor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.starter.log.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 链路追踪拦截器
 *
 * @author Young
 * @date 2020/12/16
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {

    /**
     * pre 处理
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param handler 参数 handler
     * @return 处理结果
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(HeaderConstant.TRACE_ID);
        if (StrUtil.isNotEmpty(traceId)) {
            MDC.put(LogConstant.LOG_TRACE_ID, traceId);
        } else {
            traceId = IdUtil.fastSimpleUUID();
            MDC.put(LogConstant.LOG_TRACE_ID, traceId);
        }
        return true;
    }

    /**
     * after Completion
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param handler 参数 handler
     * @param ex 参数 ex
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理 MDC 中的 traceId，防止线程池复用导致的上下文污染与内存泄漏
        MDC.remove(LogConstant.LOG_TRACE_ID);
    }
}
