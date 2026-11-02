package org.etd.framework.starter.ai.runtime.context;

import java.util.Map;
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

    private RequestContextSnapshot(RequestContextModel context, Map<String, String> mdc, Authentication authentication) {
        this.context = context;
        this.mdc = mdc;
        this.authentication = authentication;
    }

    /** @return 不含用户、租户、凭证及 MDC 的后台维护快照，不授予平台管理员或工具访问权限 */
    public static RequestContextSnapshot maintenance() {
        RequestContextModel context = new RequestContextModel();
        Map<String, String> mdc = Map.of();
        return new RequestContextSnapshot(context, mdc, null);
    }

    /** @return 当前已认证请求的内存快照，包含Token，不得序列化、记录日志或持久化 */
    public static RequestContextSnapshot capture() {
        return new RequestContextSnapshot();
    }

    /** @return 本线程的显式作用域；必须使用 try-with-resources，并在同一线程关闭 */
    public Scope openScope() {
        return new Scope();
    }

    /** 保存进入前状态，退出恢复；不接收或保存待执行流程。 */
    public final class Scope implements AutoCloseable {
        private final RequestContextModel previous = RequestContext.copyRequestContext();
        private final Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        private final SecurityContext previousSecurity = SecurityContextHolder.getContext();

        private Scope() {
            install(context.copy());
            restoreMdc(mdc);
            SecurityContext security = SecurityContextHolder.createEmptyContext();
            security.setAuthentication(authentication);
            SecurityContextHolder.setContext(security);
        }

        /** 恢复工作线程原有上下文，而不是简单清空原身份。 */
        @Override
        public void close() {
            install(previous);
            restoreMdc(previousMdc);
            SecurityContextHolder.setContext(previousSecurity);
        }
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
