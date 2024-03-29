package org.etd.framework.common.core.context;

import org.etd.framework.common.core.constants.RequestContextConstant;
import org.etd.framework.common.core.context.model.RequestContext;
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
        RequestContext.setTenantCode(getHeaderValue(e, RequestContextConstant.TENANT_CODE.getCode()));
        RequestContext.setToken(getHeaderValue(e, RequestContextConstant.TOKEN.getCode()));
        RequestContext.setRequestIP(getRemoteIp(e));
        RequestContext.setAttribute(getAttribute(e));
    }

    @Override
    public void beforeInitialization(E e) {
        RequestContext.clean();
    }

    @Override
    public void afterInitialization(E e) {

    }
}
