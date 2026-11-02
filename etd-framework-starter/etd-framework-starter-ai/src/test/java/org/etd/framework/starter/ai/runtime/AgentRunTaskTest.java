package org.etd.framework.starter.ai.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 验证慢网络不会占用生命周期锁或正文快照锁，取消与部分正文保存可以独立推进。 */
class AgentRunTaskTest {
    @Test
    void blockedOutputDoesNotBlockCancellationOrProgressSnapshot() throws Exception {
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AgentRunEventListener listener = new BlockingOutput(sending, release);
        AgentRunTask task = task(listener);
        task.output().append("部分正文", 100);
        try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> event = threads.submit(() -> task.output().emit(AgentRuntimeEventType.DELTA, "部分正文"));
            try {
                assertThat(sending.await(3, TimeUnit.SECONDS)).isTrue();
                Future<?> cancel = threads.submit(task::dispose);
                cancel.get(1, TimeUnit.SECONDS);
                Future<String> snapshot = threads.submit(() -> task.output().progress().content());
                assertThat(snapshot.get(1, TimeUnit.SECONDS)).isEqualTo("部分正文");
                assertThat(task.isDisposed()).isTrue();
                Duration wait = Duration.ofSeconds(1);
                assertThat(task.stopSignal().block(wait)).isTrue();
            } finally {
                release.countDown();
            }
            event.get(1, TimeUnit.SECONDS);
        }
    }

    private AgentRunTask task(AgentRunEventListener listener) {
        AgentChatPreparation prepared = new AgentChatPreparation("slow-output", "tenant", "model", "System",
                List.of(), null, null, List.of(), List.of(), false, null, null, null, null, Map.of());
        AgentRunStore store = mock(AgentRunStore.class);
        AgentRunLeaseStore lease = mock(AgentRunLeaseStore.class);
        AgentRunRequest request = new AgentRunRequest(prepared, "问题", store, lease, listener);
        return new AgentRunTask(request, 10);
    }

    /**
     * 模拟输出通道阻塞，不借助睡眠猜测发送时机。
     * @param sending 已进入网络发送的信号
     * @param release 测试结束时放行网络发送的信号
     */
    private record BlockingOutput(CountDownLatch sending, CountDownLatch release) implements AgentRunEventListener {
        @Override
        public void onEvent(AgentRuntimeEvent event) {
            sending.countDown();
            try {
                if (!release.await(3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("测试未放行输出通道");
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("输出线程被意外中断", failure);
            }
        }
    }
}
