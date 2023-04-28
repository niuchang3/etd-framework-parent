package org.etd.framework.starter.message.core.context;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.etd.framework.common.core.constants.RequestContextConstant;
import org.etd.framework.common.core.context.AbstractRequestContextInitialization;
import org.etd.framework.common.core.context.ContextInitialization;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.starter.log.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRabbitRequestContextInitialization extends AbstractRequestContextInitialization<Message> implements ContextInitialization<Message> {

    @Override
    protected String getHeaderValue(Message message, String headerName) {
        return (String) message.getMessageProperties().getHeaders().get(headerName);
    }

    @Override
    protected Map<String, Object> getAttribute(Message message) {
        Map<String, Object> attr = Maps.newHashMap();
        try {
            NotificationMsgRequest notificationMsgRequest = new Gson().fromJson(new String(message.getBody(), StandardCharsets.UTF_8), NotificationMsgRequest.class);
            if (!ObjectUtils.isEmpty(notificationMsgRequest) &&
                    !ObjectUtils.isEmpty(notificationMsgRequest.getRequestContextModel()) &&
                    !ObjectUtils.isEmpty(notificationMsgRequest.getRequestContextModel().getAttribute())) {
                attr.putAll(notificationMsgRequest.getRequestContextModel().getAttribute());
            }
        } finally {
            return attr;
        }
    }

    @Override
    protected String getRemoteIp(Message message) {
        return (String) message.getMessageProperties().getHeaders().get(RequestContextConstant.REQUEST_IP.getCode());
    }

    @Override
    public void beforeInitialization(Message message) {

    }

    /**
     * Request上下文初始化完成之后将链路ID（traceId） 透传给slf4j后续进行日志跟踪
     *
     * @param message
     */
    @Override
    public void afterInitialization(Message message) {
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put(LogConstant.LOG_TRACE_ID, RequestContext.getTraceId());
        MDC.setContextMap(contextMap);
        RequestContext.setUserCode(getHeaderValue(message, RequestContextConstant.USER_CODE.getCode()));
        RequestContext.setUserName(getHeaderValue(message, RequestContextConstant.USER_NAME.getCode()));
        RequestContext.setUserRole(getHeaderValue(message, RequestContextConstant.USER_ROLE.getCode()));
    }
}
