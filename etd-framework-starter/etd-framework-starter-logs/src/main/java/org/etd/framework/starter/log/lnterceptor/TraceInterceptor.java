package org.etd.framework.starter.log.lnterceptor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.etd.framework.common.core.constants.RequestContextConstant;
import org.etd.framework.starter.log.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Young
 * @description
 * @date 2020/12/16
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(RequestContextConstant.TRACE_ID.getCode());
        if (StrUtil.isNotEmpty(traceId)) {
            MDC.put(LogConstant.LOG_TRACE_ID, traceId);
        } else {
            traceId = IdUtil.fastSimpleUUID();
            MDC.put(LogConstant.LOG_TRACE_ID, traceId);
        }
        return true;
    }
}
