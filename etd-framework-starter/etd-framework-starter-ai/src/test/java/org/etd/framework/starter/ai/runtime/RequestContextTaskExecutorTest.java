package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.ai.runtime.context.RequestContextTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证公共执行入口在任务提交、周期维护及中断退出时保留上下文和取消语义。 */
class RequestContextTaskExecutorTest {
    /** 测试结束后移除调用线程身份。 */
    @AfterEach
    void clear() {
        RequestContext.clean();
    }

    /** 任务使用提交时的租户，结束后恢复工作线程原身份，不能污染后续任务。 */
    @Test
    void restoresWorkerContextAfterSubmission() throws Exception {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            seedWorker(executor);
            RequestContext.setTenantCode(1L);
            Runnable action = () -> {
                assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
                RequestContext.setTenantCode(2L);
            };
            Future<?> task = RequestContextTaskExecutor.submit(executor, action);
            RequestContext.clean();
            task.get(5, TimeUnit.SECONDS);
            assertWorkerTenant(executor);
        }
        assertThat(RequestContext.getTenantCode()).isNull();
    }

    /** 周期任务在注册时捕获上下文，原HTTP请求清理后仍能续租，退出后不遗留身份。 */
    @Test
    void preservesScheduledContextAfterCallerClearsIt() throws Exception {
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            seedWorker(executor);
            RequestContext.setTenantCode(1L);
            CompletableFuture<Long> observed = new CompletableFuture<>();
            Runnable action = () -> observed.complete(RequestContext.getTenantCode());
            ScheduledFuture<?> task = RequestContextTaskExecutor.scheduleWithFixedDelay(
                    executor, action, 0, 1, TimeUnit.SECONDS);
            try {
                RequestContext.clean();
                assertThat(observed.get(5, TimeUnit.SECONDS)).isEqualTo(1L);
            } finally {
                task.cancel(false);
            }
            assertWorkerTenant(executor);
        }
    }

    /** 调用方取消等待时，所有未结束的子任务都收到中断，调用方中断标记不能丢失。 */
    @Test
    void interruptsRemainingTasksWhenCallerIsInterrupted() throws Exception {
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch stopped = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Boolean> interrupted = new CompletableFuture<>();
        Function<Integer, Integer> action = value -> waitForRelease(value, started, stopped, release);
        Runnable callerTask = () -> invokeUntilInterrupted(action, interrupted);
        Thread caller = Thread.ofVirtual().start(callerTask);
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            caller.interrupt();
            assertThat(interrupted.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            caller.interrupt();
            caller.join(5000);
        }
    }

    /** 设置工作线程原上下文，用于确认执行器恢复而非直接清空原身份。 */
    private void seedWorker(ExecutorService executor) throws Exception {
        Runnable seed = () -> RequestContext.setTenantCode(99L);
        Future<?> task = executor.submit(seed);
        task.get(5, TimeUnit.SECONDS);
    }

    /** 绕过公共入口直接检查工作线程，确认上一任务没有泄漏请求上下文。 */
    private void assertWorkerTenant(ExecutorService executor) throws Exception {
        Callable<Long> readTenant = RequestContext::getTenantCode;
        Future<Long> tenant = executor.submit(readTenant);
        assertThat(tenant.get(5, TimeUnit.SECONDS)).isEqualTo(99L);
    }

    /** 子任务阻塞到测试主动放行或收到执行器的取消中断。 */
    private Integer waitForRelease(Integer value, CountDownLatch started, CountDownLatch stopped,
            CountDownLatch release) {
        started.countDown();
        try {
            release.await();
            return value;
        } catch (InterruptedException exception) {
            stopped.countDown();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("测试任务已取消", exception);
        }
    }

    /** 在独立调用线程执行并发批次，并记录公共执行器退出时的中断标记。 */
    private void invokeUntilInterrupted(Function<Integer, Integer> action, CompletableFuture<Boolean> interrupted) {
        List<Integer> inputs = List.of(1, 2);
        try {
            RequestContextTaskExecutor.invokeList(inputs, action);
            interrupted.complete(false);
        } catch (IllegalStateException exception) {
            interrupted.complete(Thread.currentThread().isInterrupted());
        } finally {
            RequestContext.clean();
        }
    }
}
