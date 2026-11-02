package org.etd.framework.starter.ai.agent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import org.etd.framework.starter.ai.tooling.AgentToolAuthorization;
import org.springframework.ai.tool.ToolCallback;

/** 工具执行的可信作用域与 Run 取消桥接；工具循环仍由官方节点负责。 */
final class AgentToolInterceptor extends ToolInterceptor {
    private final RequestContextSnapshot context;
    private final AgentRunControl control;
    private final Set<String> allowed = new HashSet<>();
    private final AgentToolAuthorization authorization;
    private final int maximumCalls;
    private final AtomicInteger actualCalls = new AtomicInteger();

    AgentToolInterceptor(RequestContextSnapshot context, AgentRunControl control, List<ToolCallback> callbacks,
            AgentToolAuthorization authorization, int maximumCalls) {
        this.context = context;
        this.control = control;
        this.authorization = authorization;
        this.maximumCalls = maximumCalls;
        if (callbacks != null) {
            for (ToolCallback callback : callbacks) {
                allowed.add(callback.getToolDefinition().name());
            }
        }
    }

    @Override
    public String getName() {
        return "agent_tool_guard";
    }

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        try (Scope scope = context.openScope()) {
            return invokeAuthorized(request, handler);
        }
    }

    private ToolCallResponse invokeAuthorized(ToolCallRequest request, ToolCallHandler handler) {
        control.requireActive();
        control.registerToolThread();
        try {
            requireAuthorized(request.getToolName());
            // 官方 ToolCallLimitHook 在下一次模型调用前检查，不能阻止同一消息中的批量超额执行。
            if (actualCalls.incrementAndGet() > maximumCalls) {
                control.fail();
                throw new IllegalStateException("本轮工具执行次数已达到上限");
            }
            ToolCallResponse response = handler.call(request);
            control.requireActive();
            if (response.isError()) {
                control.fail();
                throw new IllegalStateException("工具执行失败，请检查输入或目标服务");
            }
            return response;
        } catch (RuntimeException failure) {
            // 业务失败可作为明确的正常结果返回；异常不能被工具节点吞成可重试观察，取消状态仍优先保留。
            control.fail();
            throw failure;
        } finally {
            control.unregisterToolThread();
        }
    }

    private void requireAuthorized(String name) {
        try {
            if (!allowed.contains(name) || authorization == null) {
                throw new IllegalStateException("工具未授权或未配置执行授权服务");
            }
            authorization.requireAuthorized(name);
        } catch (RuntimeException failure) {
            // 官方工具节点可能把异常转换成观察结果，先终止 Run，禁止下一轮继续请求模型。
            control.fail();
            throw failure;
        }
    }
}
