package org.etd.framework.starter.event.client.core.async;

import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 事件异步任务上下文装饰器，在提交时快照安全请求上下文并在工作线程恢复。
 */
public class EventContextTaskDecorator implements TaskDecorator {

    private static final String MDC_TRACE_ID = "traceId";

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, Object> requestContext = copySafeRequestContext();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return () -> runWithContext(runnable, requestContext, mdcContext);
    }

    private void runWithContext(Runnable runnable, Map<String, Object> requestContext,
                                Map<String, String> mdcContext) {
        RequestContextInitializer.init(requestContext);
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        setTraceIdToMdc(requestContext);
        try {
            runnable.run();
        } finally {
            RequestContext.clean();
            MDC.clear();
        }
    }

    private Map<String, Object> copySafeRequestContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : RequestContextInitializer.exportMessageHeaders().entrySet()) {
            if (entry.getValue() != null) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        return context;
    }

    private void setTraceIdToMdc(Map<String, Object> requestContext) {
        Object traceId = requestContext.get(HeaderConstant.TRACE_ID);
        if (traceId != null && !String.valueOf(traceId).isBlank()) {
            MDC.put(MDC_TRACE_ID, String.valueOf(traceId));
        }
    }
}
