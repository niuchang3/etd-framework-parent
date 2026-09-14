package org.etd.framework.starter.event.client.core;

import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.event.client.core.async.EventContextTaskDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 事件异步线程请求上下文透传测试。
 */
class EventContextTaskDecoratorTest {

    @AfterEach
    void cleanRequestContext() {
        RequestContext.clean();
    }

    @Test
    void shouldPropagateSafeContextAndCleanWorkerThread() throws Exception {
        RequestContext.setTraceId("trace-async-001");
        RequestContext.setTenantCode(1001L);
        RequestContext.setToken("secret-token");
        Runnable decorated = new EventContextTaskDecorator().decorate(() -> {
            assertThat(RequestContext.getTraceId()).isEqualTo("trace-async-001");
            assertThat(RequestContext.getTenantCode()).isEqualTo(1001L);
            assertThat(RequestContext.getToken()).isNull();
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(decorated).get();
            assertThat(executor.submit(RequestContext::getTraceId).get()).isNull();
        } finally {
            executor.shutdownNow();
        }
    }
}
