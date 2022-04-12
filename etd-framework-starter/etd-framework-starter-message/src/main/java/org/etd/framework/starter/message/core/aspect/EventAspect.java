package org.etd.framework.starter.message.core.aspect;

import com.oracle.webservices.internal.api.message.PropertySet;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.common.utils.json.JsonUtils;
import org.etd.framework.starter.message.core.annotation.Event;
import org.etd.framework.starter.message.core.queue.RabbitQueue;
import org.etd.framework.starter.message.core.queue.extend.DefaultRabbitQueue;
import org.etd.framework.starter.message.core.service.RabbitMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class EventAspect {

    @Autowired
    private RabbitMessageService rabbitMessageService;

    @Pointcut(value = "@annotation(org.etd.framework.starter.message.core.annotation.Event)")
    public void pointcut() {

    }

    @After("pointcut()")
    public void after(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Event declaredAnnotation = method.getDeclaredAnnotation(Event.class);
        String gson = JsonUtils.toGson(joinPoint.getArgs());
        for (String code : declaredAnnotation.messageHandleCode()) {
            NotificationMsgRequest<String> request = new NotificationMsgRequest<>();
            request.setMessageHandleCode(code);
            request.setMessageBody(gson);
            request.setRetries(declaredAnnotation.retries());
            request.setRequestContextModel(RequestContext.getRequestContext());
            rabbitMessageService.sendMessage(DefaultRabbitQueue.DEFAULT,request);
        }

    }
}
