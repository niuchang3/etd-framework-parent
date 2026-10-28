package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.model.client.ModelRequestHeaders;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEvent;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeEventType;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.runtime.model.AgentModelInput;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.etd.framework.starter.ai.runtime.spi.AgentOutputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;
import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

/** 通用运行容器；业务通过检索、生命周期和输出插槽接入，框架负责线程、上下文、超时及取消。 */
public class AgentRuntime implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AgentRuntime.class);
    /** 无业务含义的统一运行失败协议，由业务插槽决定最终对外错误。 */
    public static final String EXECUTION_FAILED = "AGENT_EXECUTION_FAILED";
    private final AgentExecutionEngine engine;
    private final AgentRuntimeProperties properties;
    private final Supplier<List<AgentRunRecordService>> recordServices;
    private final Semaphore capacity;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService clock = Executors.newScheduledThreadPool(2);
    private final Map<String, AgentRunTask> active = new ConcurrentHashMap<>();

    /** @param engine 无状态执行引擎 @param properties 通用运行边界 @param recordServices 延迟取得业务生命周期实现，避免装配循环 */
    public AgentRuntime(AgentExecutionEngine engine, AgentRuntimeProperties properties,
            Supplier<List<AgentRunRecordService>> recordServices) {
        this.engine = engine;
        this.properties = properties;
        this.recordServices = recordServices;
        capacity = new Semaphore(properties.getMaxConcurrentRuns());
        clock.scheduleWithFixedDelay(this::finishExpiredRunList, 15, 15, TimeUnit.SECONDS);
    }

    /** 在容量允许后调用业务准备插槽，避免拒绝请求留下运行记录。
     * @param preparation 短事务创建执行并提供本轮插槽
     * @return 本节点运行句柄
     * @throws RuntimeException 业务校验、容量或任务提交失败
     */
    public AgentRunControl start(Supplier<AgentRunRequest> preparation) {
        if (!capacity.tryAcquire()) {
            throw new ApiRuntimeException("当前生成任务较多，请稍后再试");
        }
        AgentRunTask state = null;
        try {
            AgentRunRequest runRequest = preparation.get();
            state = new AgentRunTask(runRequest);
            RequestContextSnapshot context = RequestContextSnapshot.capture();
            AgentRunTask task = state;
            active.put(state.executionId(), state);
            workers.submit(() -> context.run(() -> execute(task, context)));
            return state;
        } catch (RuntimeException exception) {
            capacity.release();
            if (state != null) {
                active.remove(state.executionId());
                finish(state, AgentRuntimeStatus.FAILED);
                state.runRequest.eventListener().close();
            }
            throw exception;
        }
    }

    private void execute(AgentRunTask state, RequestContextSnapshot context) {
        state.worker = Thread.currentThread();
        ScheduledFuture<?> maintenance = clock.scheduleWithFixedDelay(() -> context.run(() -> maintain(state)), 1, 1, TimeUnit.SECONDS);
        try {
            executeAnswer(state);
        } catch (Exception exception) {
            LOG.warn("Agent运行异常，executionId={}，异常类型={}", state.executionId(), exception.getClass().getSimpleName());
            Thread.interrupted();
            if (exception instanceof AgentOutputException) {
                state.cancel();
            }
            if (!state.finished.get()) {
                AgentRuntimeStatus desired = state.stopStatus.get();
                finish(state, desired == null ? AgentRuntimeStatus.FAILED : desired);
            }
        } finally {
            state.finished.set(true);
            maintenance.cancel(false);
            active.remove(state.executionId());
            capacity.release();
            state.runRequest.eventListener().close();
        }
    }

    private void executeAnswer(AgentRunTask state) {
        emit(state, AgentRuntimeEventType.START, null, null);
        requireNotStopped(state);
        AgentModelInput prepared = engine.prepare(state.runRequest.chatRequest());
        state.references = state.runRequest.chatRequest().citationsEnabled() ? prepared.references() : List.of();
        emit(state, AgentRuntimeEventType.REFERENCES, null, null);
        requireNotStopped(state);
        consume(state, prepared);
        AgentRuntimeStatus desired = state.stopStatus.get();
        finish(state, desired == null ? AgentRuntimeStatus.SUCCEEDED : desired);
    }

    private void consume(AgentRunTask state, AgentModelInput prepared) {
        ChatModel model = state.runRequest.model().get();
        Map<String, String> headers = ModelRequestHeaders.capture();
        Mono<Boolean> cancellation = state.cancellation.asMono();
        Flux<ChatResponse> stream = engine.stream(model, prepared).takeUntilOther(cancellation)
                .contextWrite(context -> context.put(ModelRequestHeaders.REACTOR_KEY, headers));
        // 错误以有序信号处理，防止快速失败丢弃最后一段文本；关闭Stream必须取消上游订阅。
        try (Stream<Signal<ChatResponse>> responses = stream.materialize().toStream(1)) {
            Iterator<Signal<ChatResponse>> iterator = responses.iterator();
            while (iterator.hasNext()) {
                requireNotStopped(state);
                Signal<ChatResponse> signal = iterator.next();
                if (signal.isOnError()) {
                    throw new IllegalStateException("模型调用失败");
                }
                if (signal.hasValue()) {
                    appendResponse(state, signal.get());
                }
            }
        }
        if (state.content().isEmpty() && state.stopStatus.get() == null) {
            throw new IllegalStateException("模型未返回可展示正文");
        }
    }

    private void appendResponse(AgentRunTask state, ChatResponse response) {
        recordUsage(state, response);
        if (response.getResult() == null || response.getResult().getOutput() == null) {
            return;
        }
        String text = response.getResult().getOutput().getText();
        if (text != null && !text.isEmpty()) {
            state.append(text, properties.getMaxResponseCharacters());
            emit(state, AgentRuntimeEventType.DELTA, text, null);
        }
    }

    private void recordUsage(AgentRunTask state, ChatResponse response) {
        Usage usage = response.getMetadata().getUsage();
        if (usage != null) {
            if (usage.getPromptTokens() != null && usage.getPromptTokens() > 0) {
                state.inputTokens = usage.getPromptTokens().longValue();
            }
            if (usage.getCompletionTokens() != null && usage.getCompletionTokens() > 0) {
                state.outputTokens = usage.getCompletionTokens().longValue();
            }
        }
    }

    private void maintain(AgentRunTask state) {
        if (state.finished.get()) {
            return;
        }
        try {
            if (System.nanoTime() - state.startedNanos >= TimeUnit.SECONDS.toNanos(properties.getTimeoutSeconds())) {
                state.stop(AgentRuntimeStatus.FAILED);
                return;
            }
            AgentRuntimeStatus status = state.runRequest.recordService().renewRunLease(state.executionId());
            if (status != AgentRuntimeStatus.RUNNING) {
                if (status != AgentRuntimeStatus.SUCCEEDED) {
                    state.stop(status);
                }
                return;
            }
            checkpointAndHeartbeat(state);
        } catch (AgentOutputException exception) {
            state.cancel();
        } catch (Exception exception) {
            state.stop(AgentRuntimeStatus.FAILED);
        }
    }

    private void checkpointAndHeartbeat(AgentRunTask state) {
        long now = System.nanoTime();
        if (now - state.lastHeartbeatNanos >= TimeUnit.SECONDS.toNanos(properties.getHeartbeatSeconds())) {
            emit(state, AgentRuntimeEventType.HEARTBEAT, null, null);
            state.lastHeartbeatNanos = now;
        }
        if (now - state.lastCheckpointNanos >= TimeUnit.MILLISECONDS.toNanos(properties.getCheckpointMillis())) {
            AgentRunProgress progress = state.progress();
            state.runRequest.recordService().saveRunProgress(progress);
            state.lastCheckpointNanos = now;
        }
    }

    private void finish(AgentRunTask state, AgentRuntimeStatus desired) {
        // 取消会中断工作线程，持久化之前清除中断位，保证业务JDBC插槽可以提交终态。
        Thread.interrupted();
        AgentRunProgress progress = state.progress();
        String code = desired == AgentRuntimeStatus.FAILED ? EXECUTION_FAILED : null;
        String message = desired == AgentRuntimeStatus.FAILED ? "回答生成失败或超时，请检查模型和知识库配置后重试" : null;
        AgentRuntimeResult result = new AgentRuntimeResult(progress, desired, code, message);
        AgentRuntimeResult actual = state.runRequest.recordService().finishRun(result);
        state.finished.set(true);
        if (!state.disconnected.get()) {
            emit(state, AgentRuntimeEventType.TERMINAL, null, actual);
        }
    }

    private void emit(AgentRunTask state, AgentRuntimeEventType type, String text, AgentRuntimeResult result) {
        // 增量事件不复制累计正文，避免长回答产生平方级字符串分配；完整正文只交给持久化及终态插槽。
        AgentRunProgress progress = result == null ? state.eventSnapshot() : result.progress();
        AgentRuntimeEvent event = new AgentRuntimeEvent(type, state.sequence.incrementAndGet(), progress, text, result,
                state.runRequest.attributes());
        state.runRequest.eventListener().onEvent(event);
    }

    private void requireNotStopped(AgentRunTask state) {
        if (state.stopStatus.get() != null) {
            throw new IllegalStateException("本轮执行已停止");
        }
    }

    private void finishExpiredRunList() {
        for (AgentRunRecordService recordService : recordServices.get()) {
            try {
                recordService.finishExpiredRunList();
            } catch (Exception exception) {
                LOG.warn("Agent运行回收失败，异常类型={}", exception.getClass().getSimpleName());
            }
        }
    }

    /** 应用关闭取消本节点执行，持久化失败由业务生命周期插槽回收。 */
    @Override
    public void close() {
        for (AgentRunTask state : active.values()) {
            state.cancel();
        }
        workers.shutdown();
        try {
            workers.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        workers.shutdownNow();
        clock.shutdownNow();
    }
}
