package org.etd.framework.starter.ai.tooling;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import com.alibaba.cloud.ai.graph.agent.tool.AsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellableAsyncToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 官方异步工具的必要取消桥：官方节点局部令牌未连接整轮取消，不替换工具节点或调用循环。 */
public final class AgentAsyncToolCallback implements CancellableAsyncToolCallback {
    private static final Logger LOG = LoggerFactory.getLogger(AgentAsyncToolCallback.class);
    private final AsyncToolCallback delegate;
    private final AgentRunControl control;
    private final RequestContextSnapshot context;

    /** @param delegate 官方异步工具 @param control 当前 Run 的取消和剩余期限 */
    public AgentAsyncToolCallback(AsyncToolCallback delegate, AgentRunControl control) {
        this.delegate = delegate;
        this.control = control;
        this.context = RequestContextSnapshot.capture();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public Duration getTimeout() {
        Duration remaining = control.remainingTime();
        Duration ownTimeout = delegate.getTimeout();
        return remaining.compareTo(ownTimeout) < 0 ? remaining : ownTimeout;
    }

    @Override
    public String call(String arguments) {
        Map<String, Object> values = Map.of();
        ToolContext context = new ToolContext(values);
        return call(arguments, context);
    }

    @Override
    public CompletableFuture<String> callAsync(String arguments, ToolContext context, CancellationToken nativeToken) {
        control.requireActive();
        AsyncCancellation cancellation = new AsyncCancellation(this.context, control.runId());
        // 先登记原生取消入口，再进入工具；Future 可在取消之后才返回。
        control.cancellationToken().onCancel(cancellation::cancel);
        nativeToken.onCancel(cancellation::cancel);
        CompletableFuture<String> future;
        if (delegate instanceof CancellableAsyncToolCallback cancellable) {
            future = cancellable.callAsync(arguments, context, cancellation.token);
        } else {
            future = delegate.callAsync(arguments, context);
        }
        if (future == null) {
            throw new IllegalStateException("异步工具未返回 Future");
        }
        cancellation.attach(future);
        return future;
    }

    /** 只保存取消状态、Future 与身份快照，不能把执行流程存入上下文。 */
    private static final class AsyncCancellation {
        private final DefaultCancellationToken token = new DefaultCancellationToken();
        private final AtomicReference<CompletableFuture<String>> future = new AtomicReference<>();
        private final RequestContextSnapshot context;
        private final String runId;

        private AsyncCancellation(RequestContextSnapshot context, String runId) {
            this.context = context;
            this.runId = runId;
        }

        private void attach(CompletableFuture<String> pending) {
            future.set(pending);
            if (token.isCancelled()) {
                try (Scope scope = context.openScope()) {
                    pending.cancel(true);
                }
            }
        }

        private void cancel() {
            CompletableFuture<String> completed = future.get();
            if (completed != null && completed.isDone()) {
                return;
            }
            try (Scope scope = context.openScope()) {
                try {
                    token.cancel();
                } catch (RuntimeException failure) {
                    // 单个工具的回调失败不能阻止本 Future 或同 Run 其他工具收到取消。
                    LOG.warn("异步工具取消回调异常，runId={}，异常类型={}", runId, failure.getClass().getSimpleName());
                } finally {
                    CompletableFuture<String> pending = future.get();
                    if (pending != null) {
                        pending.cancel(true);
                    }
                }
            }
        }
    }
}
