package org.etd.framework.common.core.context;

import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.UUID;

/**
 * 抽象的请求上下文处理程序
 *
 * @param <E>
 * @author 牛昌
 */
public abstract class AbstractRequestContextInitialization<E> extends AbstractContextInitialization<E> {

    /**
     * 获取 HeaderValue
     */
    protected abstract String getHeaderValue(E e, String headerName);

    /**
     * 获取请求参数
     */
    protected abstract Map<String, Object> getAttribute(E e);

    /**
     * 获取请求 IP
     */
    protected abstract String getRemoteIp(E e);


    @Override
    public void invoke(E e) {

        String traceId = getHeaderValue(e, HeaderConstant.TRACE_ID);
        RequestContext.setTraceId(ObjectUtils.isEmpty(traceId) ? UUID.randomUUID().toString() : traceId);

        String tenantCode = getHeaderValue(e, HeaderConstant.TENANT_CODE);
        if (!ObjectUtils.isEmpty(tenantCode)) {
            try {
                RequestContext.setTenantCode(Long.valueOf(tenantCode));
            } catch (NumberFormatException ignored) {
            }
        }

        RequestContext.setToken(getHeaderValue(e, HeaderConstant.AUTHORIZATION));
        RequestContext.setLanguage(getHeaderValue(e, HeaderConstant.ACCEPT_LANGUAGE));
        RequestContext.setApplicationName(getHeaderValue(e, HeaderConstant.APPLICATION_NAME));
        RequestContext.setApplicationVersion(getHeaderValue(e, HeaderConstant.APPLICATION_VERSION));
        RequestContext.setDeviceFingerprint(getHeaderValue(e, HeaderConstant.DEVICE_FINGERPRINT));
        RequestContext.setDeviceId(getHeaderValue(e, HeaderConstant.DEVICE_ID));
        RequestContext.setUserAgent(getHeaderValue(e, HeaderConstant.USER_AGENT));

        RequestContext.setRequestIP(getRemoteIp(e));
        RequestContext.setAttribute(getAttribute(e));

        SecurityContext context = SecurityContextHolder.getContext();
        if (ObjectUtils.isEmpty(context.getAuthentication())) {
            return;
        }
        if (ObjectUtils.isEmpty(context.getAuthentication().getDetails())) {
            return;
        }
        if (context.getAuthentication().getDetails() instanceof UserDetails details) {
            RequestContext.setUser(details);
        }
    }

    @Override
    public void beforeInitialization(E e) {
        RequestContext.clean();
    }

    @Override
    public void afterInitialization(E e) {

    }
}
