package org.etd.framework.starter.message.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.starter.message.core.annotation.Event;
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
        System.out.println( declaredAnnotation.messageHandleCode());
//        rabbitMessageService.sendMessage();
    }
}
