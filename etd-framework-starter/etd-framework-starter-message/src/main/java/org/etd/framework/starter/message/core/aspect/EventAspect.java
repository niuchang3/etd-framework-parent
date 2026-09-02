package org.etd.framework.starter.message.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.common.utils.json.JsonUtils;
import org.etd.framework.starter.message.core.annotation.Event;
import org.etd.framework.starter.message.core.queue.extend.DefaultRabbitQueue;
import org.etd.framework.starter.message.core.service.RabbitMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 业务事件发布切面拦截器。
 * <p>
 * 监听带有 {@link Event} 注解的方法，在方法成功执行后自动组装消息并发送至 RabbitMQ 消息队列。
 */
@Aspect
@Component
public class EventAspect {

    @Autowired
    private RabbitMessageService rabbitMessageService;

    @Pointcut(value = "@annotation(org.etd.framework.starter.message.core.annotation.Event)")
    /**
     * pointcut
     *
     */
    public void pointcut() {

    }

    @After("pointcut()")
    /**
     * after
     *
     * @param joinPoint 参数 joinPoint
     */
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
            rabbitMessageService.sendMessage(DefaultRabbitQueue.DEFAULT, request);
        }

    }
}
