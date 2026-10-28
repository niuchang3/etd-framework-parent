package org.etd.framework.starter.ai.runtime.context;

import java.util.Map;
import java.util.function.Supplier;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.context.model.RequestContextModel;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

/** 请求级上下文快照；跨线程调用时独立恢复身份、消息头及MDC，并在退出时恢复工作线程原状态。 */
public final class RequestContextSnapshot {
    private final RequestContextModel context;
    private final Map<String, String> mdc;
    private final Authentication authentication;

    private RequestContextSnapshot() {
        context = RequestContext.copyRequestContext();
        mdc = MDC.getCopyOfContextMap();
        authentication = SecurityContextHolder.getContext().getAuthentication();
    }

    /** @return 当前已认证请求的内存快照，包含Token，不得序列化、记录日志或持久化 */
    public static RequestContextSnapshot capture() {
        return new RequestContextSnapshot();
    }

    /** @param action 当前调用 @return 调用结果 @throws RuntimeException 原样传播调用失败 */
    public <T> T call(Supplier<T> action) {
        RequestContextModel previous = RequestContext.copyRequestContext();
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        SecurityContext previousSecurity = SecurityContextHolder.getContext();
        install(context.copy());
        restoreMdc(mdc);
        SecurityContext security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(authentication);
        SecurityContextHolder.setContext(security);
        try {
            return action.get();
        } finally {
            install(previous);
            restoreMdc(previousMdc);
            SecurityContextHolder.setContext(previousSecurity);
        }
    }

    /** @param action 在快照上下文中执行的任务；嵌套调用退出时恢复外层上下文 */
    public void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }

    private void install(RequestContextModel value) {
        RequestContext.clean();
        RequestContextModel target = RequestContext.getRequestContext();
        target.setHeaders(value.getHeaders());
        target.setUserDetails(value.getUserDetails());
        target.setControlFlags(value.getControlFlags());
    }

    private void restoreMdc(Map<String, String> value) {
        MDC.clear();
        if (value != null) {
            MDC.setContextMap(value);
        }
    }
}
