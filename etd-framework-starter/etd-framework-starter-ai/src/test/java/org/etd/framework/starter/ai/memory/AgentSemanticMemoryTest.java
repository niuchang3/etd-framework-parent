package org.etd.framework.starter.ai.memory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.memory.semantic.AgentSemanticMemory;
import org.etd.framework.starter.ai.memory.semantic.EmbeddingSpace;
import org.springframework.ai.document.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证真实多库并发、子任务身份、失败及时传播及整批取消，避免退回通用 Function 执行包装。 */
class AgentSemanticMemoryTest {
    @Test
    void interruptionCancelsEveryOutstandingKnowledgeQuery() throws Exception {
        BlockingKnowledge source = new BlockingKnowledge(false);
        CompletableFuture<Throwable> result = new CompletableFuture<>();
        CompletableFuture<Boolean> interrupted = new CompletableFuture<>();
        Thread parent = Thread.ofVirtual().start(() -> runBatch(source, result, interrupted));
        try {
            assertThat(source.started.await(3, TimeUnit.SECONDS)).isTrue();
            parent.interrupt();
            assertThat(result.get(3, TimeUnit.SECONDS)).isInstanceOf(IllegalStateException.class);
            assertThat(interrupted.get(3, TimeUnit.SECONDS)).isTrue();
            assertThat(source.stopped.await(3, TimeUnit.SECONDS)).isTrue();
        } finally {
            source.release.countDown();
            parent.interrupt();
            parent.join(3000);
        }
    }

    @Test
    void deniedSecondQueryCancelsBlockedFirstQueryWithoutWaitingForItsResult() throws Exception {
        BlockingKnowledge source = new BlockingKnowledge(true);
        CompletableFuture<Throwable> result = new CompletableFuture<>();
        CompletableFuture<Boolean> interrupted = new CompletableFuture<>();
        Thread parent = Thread.ofVirtual().start(() -> runBatch(source, result, interrupted));
        try {
            assertThat(result.get(3, TimeUnit.SECONDS)).isInstanceOf(SecurityException.class).hasMessage("无权限");
            assertThat(source.firstStopped.await(3, TimeUnit.SECONDS)).isTrue();
        } finally {
            source.release.countDown();
            parent.interrupt();
            parent.join(3000);
        }
    }

    private void runBatch(BlockingKnowledge source, CompletableFuture<Throwable> result,
            CompletableFuture<Boolean> interrupted) {
        RequestContext.setTenantCode(7L);
        AgentSemanticMemory memory = new AgentSemanticMemory();
        List<Long> ids = List.of(1L, 2L);
        try {
            memory.queryKnowledgeList(ids, "问题", source);
            result.complete(null);
        } catch (RuntimeException failure) {
            result.complete(failure);
            interrupted.complete(Thread.currentThread().isInterrupted());
        } finally {
            RequestContext.clean();
        }
    }

    /** 数据读取替身只接收范围和缓存，不接收待执行函数。 */
    private static final class BlockingKnowledge implements AgentKnowledgeQueryService {
        private final boolean denySecond;
        private final CountDownLatch started = new CountDownLatch(2);
        private final CountDownLatch firstStarted = new CountDownLatch(1);
        private final CountDownLatch stopped = new CountDownLatch(2);
        private final CountDownLatch firstStopped = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private BlockingKnowledge(boolean denySecond) {
            this.denySecond = denySecond;
        }

        @Override
        public void requireAuthorized(List<Long> ids) {
        }

        @Override
        public List<Document> queryKnowledge(Long id, String question, Map<EmbeddingSpace, float[]> vectors) {
            assertThat(RequestContext.getTenantCode()).isEqualTo(7L);
            started.countDown();
            try {
                if (id == 1L) {
                    firstStarted.countDown();
                }
                if (denySecond && id == 2L) {
                    if (!firstStarted.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("首个任务未开始");
                    }
                    throw new SecurityException("无权限");
                }
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("未收到取消");
                }
                return List.of();
            } catch (InterruptedException failure) {
                stopped.countDown();
                if (id == 1L) {
                    firstStopped.countDown();
                }
                Thread.currentThread().interrupt();
                throw new IllegalStateException("查询已取消", failure);
            }
        }
    }
}
