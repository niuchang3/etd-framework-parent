package org.etd.framework.common.core.context;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.common.core.constants.RequestContextConstant;
import org.etd.framework.common.core.context.model.RequestContext;
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
     * 获取HeaderValue
     *
     * @param e
     * @param headerName
     * @return
     */
    protected abstract String getHeaderValue(E e, String headerName);

    /**
     * 获取请求参数
     *
     * @param e
     * @return
     */
    protected abstract Map<String, Object> getAttribute(E e);

    /**
     * 获取请求IP
     *
     * @param e
     * @return
     */
    protected abstract String getRemoteIp(E e);


    @Override
    public void invoke(E e) {

        String traceId = getHeaderValue(e, RequestContextConstant.TRACE_ID.getCode());
        RequestContext.setTraceId(ObjectUtils.isEmpty(traceId) ? UUID.randomUUID().toString() : traceId);

        String tenantCode = getHeaderValue(e, RequestContextConstant.TENANT_CODE.getCode());
        if(!ObjectUtils.isEmpty(tenantCode)){
            RequestContext.setTenantCode(Long.valueOf(tenantCode));
        }

        RequestContext.setToken(getHeaderValue(e, RequestContextConstant.TOKEN.getCode()));
        RequestContext.setRequestIP(getRemoteIp(e));
        RequestContext.setAttribute(getAttribute(e));

        SecurityContext context = SecurityContextHolder.getContext();
        if (ObjectUtils.isEmpty(context.getAuthentication())) {
            return;
        }
        if (ObjectUtils.isEmpty(context.getAuthentication().getDetails())) {
            return;
        }
        if (context.getAuthentication().getDetails() instanceof UserDetails) {
            UserDetails details = (UserDetails) context.getAuthentication().getDetails();
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
