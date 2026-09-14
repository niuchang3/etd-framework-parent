package org.etd.framework.starter.event.client.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.starter.event.client.annotation.Event;
import org.etd.framework.starter.event.client.core.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 解析 {@link Event} 并在业务成功后发布事件。
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class EventAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventAspect.class);

    private static final String RESULT_VARIABLE = "result";

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();

    private final EventPublisher eventPublisher;

    public EventAspect(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 业务方法正常返回后解析注解；解析或发布失败只记录日志，不改变业务返回结果。
     */
    @Around("@annotation(event)")
    public Object publishEvent(ProceedingJoinPoint joinPoint, Event event) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            EventPublication publication = resolvePublication(joinPoint, event, result);
            publishAfterTransactionCommit(publication);
        } catch (RuntimeException exception) {
            LOGGER.error("注解事件解析失败 eventType={}, method={}",
                    event.type(), joinPoint.getSignature().toShortString(), exception);
        }
        return result;
    }

    private EventPublication resolvePublication(ProceedingJoinPoint joinPoint, Event event, Object result) {
        Method method = resolveMethod(joinPoint);
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
        context.setVariable(RESULT_VARIABLE, result);
        Object payload = expressionParser.parseExpression(event.payload()).getValue(context);
        String partitionKey = evaluatePartitionKey(event.partitionKey(), context);
        return new EventPublication(event.type(), event.version(), partitionKey, payload);
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        Method signatureMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method specificMethod = AopUtils.getMostSpecificMethod(signatureMethod, joinPoint.getTarget().getClass());
        return BridgeMethodResolver.findBridgedMethod(specificMethod);
    }

    private String evaluatePartitionKey(String expression, MethodBasedEvaluationContext context) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        return value == null ? null : String.valueOf(value);
    }

    private void publishAfterTransactionCommit(EventPublication publication) {
        if (!isTransactionActive()) {
            publishSafely(publication);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishSafely(publication);
            }
        });
    }

    private boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
    }

    private void publishSafely(EventPublication publication) {
        try {
            eventPublisher.publish(publication.eventType(), publication.eventVersion(),
                    publication.partitionKey(), publication.payload());
        } catch (RuntimeException exception) {
            LOGGER.error("注解事件发布失败 eventType={}", publication.eventType(), exception);
        }
    }

    /**
     * 保存一次已完成表达式求值、等待发布的事件参数。
     */
    private record EventPublication(
            String eventType,
            int eventVersion,
            String partitionKey,
            Object payload
    ) {
    }
}
