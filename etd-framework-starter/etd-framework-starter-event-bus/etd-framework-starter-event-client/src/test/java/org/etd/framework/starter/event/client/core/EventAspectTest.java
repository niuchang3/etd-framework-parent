package org.etd.framework.starter.event.client.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.starter.event.client.annotation.Event;
import org.etd.framework.starter.event.client.core.aspect.EventAspect;
import org.etd.framework.starter.event.client.core.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 注解事件发布切面测试。
 */
class EventAspectTest {

    @Test
    void shouldResolveResultAndNamedParameterExpressions() throws Throwable {
        EventPublisher publisher = mock(EventPublisher.class);
        EventAspect aspect = new EventAspect(publisher);
        TestEventService target = new TestEventService();
        Method method = TestEventService.class.getMethod("createUser", TestCommand.class);
        TestResult result = new TestResult(1001L, "young");
        ProceedingJoinPoint joinPoint = createJoinPoint(target, method,
                new Object[]{new TestCommand(1001L)}, result);

        Object actual = aspect.publishEvent(joinPoint, method.getAnnotation(Event.class));

        assertThat(actual).isSameAs(result);
        verify(publisher).publish("upms.user.created", 1, "1001", result);
    }

    @Test
    void shouldPublishOnlyAfterActiveTransactionCommits() throws Throwable {
        EventPublisher publisher = mock(EventPublisher.class);
        EventAspect aspect = new EventAspect(publisher);
        TestEventService target = new TestEventService();
        Method method = TestEventService.class.getMethod("createUser", TestCommand.class);
        TestResult result = new TestResult(1002L, "etd");
        ProceedingJoinPoint joinPoint = createJoinPoint(target, method,
                new Object[]{new TestCommand(1002L)}, result);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        try {
            aspect.publishEvent(joinPoint, method.getAnnotation(Event.class));
            verifyNoInteractions(publisher);

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.getFirst().afterCommit();

            verify(publisher).publish("upms.user.created", 1, "1002", result);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void shouldNotPublishWhenBusinessMethodThrowsException() throws Throwable {
        EventPublisher publisher = mock(EventPublisher.class);
        EventAspect aspect = new EventAspect(publisher);
        TestEventService target = new TestEventService();
        Method method = TestEventService.class.getMethod("createUser", TestCommand.class);
        ProceedingJoinPoint joinPoint = createJoinPoint(target, method,
                new Object[]{new TestCommand(1003L)}, null);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("业务执行失败"));

        assertThatThrownBy(() -> aspect.publishEvent(joinPoint, method.getAnnotation(Event.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("业务执行失败");
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldCacheParsedSpelExpressions() throws Throwable {
        EventPublisher publisher = mock(EventPublisher.class);
        EventAspect aspect = new EventAspect(publisher);
        TestEventService target = new TestEventService();
        Method method = TestEventService.class.getMethod("createUser", TestCommand.class);
        TestResult result = new TestResult(1004L, "cache");
        ProceedingJoinPoint joinPoint = createJoinPoint(target, method,
                new Object[]{new TestCommand(1004L)}, result);
        Event event = method.getAnnotation(Event.class);

        aspect.publishEvent(joinPoint, event);
        aspect.publishEvent(joinPoint, event);

        Map<?, ?> expressionCache = (Map<?, ?>) ReflectionTestUtils.getField(aspect, "expressionCache");
        assertThat(expressionCache).hasSize(2);
    }

    private ProceedingJoinPoint createJoinPoint(Object target, Method method,
                                                Object[] arguments, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(arguments);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(result);
        when(signature.getMethod()).thenReturn(method);
        when(signature.toShortString()).thenReturn(method.toGenericString());
        return joinPoint;
    }

    /**
     * 测试用注解事件服务。
     */
    static class TestEventService {

        @Event(type = "upms.user.created", payload = "#result", partitionKey = "#command.userId")
        public TestResult createUser(TestCommand command) {
            return new TestResult(command.userId(), "young");
        }
    }

    record TestCommand(Long userId) {
    }

    record TestResult(Long userId, String username) {
    }
}
