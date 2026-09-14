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
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析 {@link Event} 并在业务成功后发布事件。
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class EventAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventAspect.class);

    private static final String RESULT_VARIABLE = "result";

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * 注解表达式在应用运行期间保持不变，缓存编译结果可避免每次方法调用都重新解析。
     */
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

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
            PendingEvent pendingEvent = resolvePendingEvent(joinPoint, event, result);
            publishAfterTransactionCommit(pendingEvent);
        } catch (RuntimeException exception) {
            LOGGER.error("注解事件解析失败 eventType={}, method={}",
                    event.type(), joinPoint.getSignature().toShortString(), exception);
        }
        return result;
    }

    private PendingEvent resolvePendingEvent(ProceedingJoinPoint joinPoint, Event event, Object result) {
        Method method = resolveMethod(joinPoint);
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
        context.setVariable(RESULT_VARIABLE, result);
        Object payload = getExpression(event.payload()).getValue(context);
        String partitionKey = evaluatePartitionKey(event.partitionKey(), context);
        return new PendingEvent(event.type(), event.version(), partitionKey, payload);
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
        Object value = getExpression(expression).getValue(context);
        return value == null ? null : String.valueOf(value);
    }

    private Expression getExpression(String expression) {
        return expressionCache.computeIfAbsent(expression, expressionParser::parseExpression);
    }

    private void publishAfterTransactionCommit(PendingEvent pendingEvent) {
        if (!isTransactionActive()) {
            publishSafely(pendingEvent);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishSafely(pendingEvent);
            }
        });
    }

    private boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
    }

    private void publishSafely(PendingEvent pendingEvent) {
        try {
            eventPublisher.publish(pendingEvent.eventType(), pendingEvent.eventVersion(),
                    pendingEvent.partitionKey(), pendingEvent.payload());
        } catch (RuntimeException exception) {
            LOGGER.error("注解事件发布失败 eventType={}", pendingEvent.eventType(), exception);
        }
    }

    /**
     * 保存一次已完成表达式求值、等待发布的事件参数。
     */
    private record PendingEvent(
            String eventType,
            int eventVersion,
            String partitionKey,
            Object payload
    ) {
    }
}
