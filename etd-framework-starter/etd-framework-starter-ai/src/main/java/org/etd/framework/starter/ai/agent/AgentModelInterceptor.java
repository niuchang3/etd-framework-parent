package org.etd.framework.starter.ai.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.etd.framework.starter.ai.memory.working.AgentWorkingMemory;
import org.etd.framework.starter.ai.model.client.ModelRequestHeaders;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.etd.framework.starter.ai.runtime.context.RequestContextFlux;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

/** 官方模型拦截器的项目约束：逐次调用硬预算、上下文及图过滤之前的原始用量。 */
final class AgentModelInterceptor extends ModelInterceptor {
    private final int budget;
    private final Message protectedMessage;
    private final RequestContextSnapshot context;
    private final AgentRunControl control;
    private final Map<String, String> headers;
    private final AgentWorkingMemory memory = new AgentWorkingMemory();

    AgentModelInterceptor(int budget, Message protectedMessage, RequestContextSnapshot context, AgentRunControl control) {
        this.budget = budget;
        this.protectedMessage = protectedMessage;
        this.context = context;
        this.control = control;
        this.headers = ModelRequestHeaders.capture();
    }

    @Override
    public String getName() {
        return "agent_model_guard";
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        control.requireActive();
        ModelRequest bounded = fitRequest(request);
        control.nextCallRound();
        ModelResponse response;
        try (Scope scope = context.openScope()) {
            response = handler.call(bounded);
        }
        if (!(response.getMessage() instanceof Flux<?> raw)) {
            throw new IllegalStateException("Agent 必须使用官方流式模型调用");
        }
        Flux<ChatResponse> typed = raw.cast(ChatResponse.class).doOnNext(this::recordUsage);
        RequestContextFlux<ChatResponse> scoped = new RequestContextFlux<>(typed, context);
        Flux<ChatResponse> result = scoped.contextWrite(this::installHeaders);
        return ModelResponse.of(result);
    }

    private Context installHeaders(Context current) {
        return current.put(ModelRequestHeaders.REACTOR_KEY, headers);
    }

    private ModelRequest fitRequest(ModelRequest request) {
        List<Message> messages = new ArrayList<>(request.getMessages());
        int fixed = request.getSystemMessage() == null ? 0 : memory.countTokens(request.getSystemMessage().getText());
        if (request.getOptions() != null && request.getOptions().getToolCallbacks() != null) {
            fixed += countToolTokens(request.getOptions().getToolCallbacks());
        }
        fixed += countToolTokens(request.getDynamicToolCallbacks());
        // 只移除最早的完整用户轮次；当前问题及其所有工具观察必须保留，禁止从中间切断工具对。
        while (fixed + memory.countMessages(messages) > budget) {
            int nextUser = findNextUser(messages);
            // 用户可连续提交相同问题，必须定位本轮最后一条匹配消息，不能误保护旧轮次。
            int protectedIndex = messages.lastIndexOf(protectedMessage);
            if (nextUser < 0 || protectedIndex < 0 || nextUser > protectedIndex) {
                throw new IllegalArgumentException("本次模型调用的必要上下文超出输入 Token 预算");
            }
            messages.subList(0, nextUser).clear();
        }
        return ModelRequest.builder(request).messages(messages).build();
    }

    private int findNextUser(List<Message> messages) {
        for (int index = 1; index < messages.size(); index++) {
            if (messages.get(index) instanceof UserMessage) {
                return index;
            }
        }
        return -1;
    }

    private int countToolTokens(List<ToolCallback> callbacks) {
        int count = 0;
        if (callbacks != null) {
            for (ToolCallback callback : callbacks) {
                count += memory.countTokens(callback.getToolDefinition().name());
                count += memory.countTokens(callback.getToolDefinition().description());
                count += memory.countTokens(callback.getToolDefinition().inputSchema());
            }
        }
        return count;
    }

    private void recordUsage(ChatResponse response) {
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            Usage usage = response.getMetadata().getUsage();
            Long prompt = usage.getPromptTokens() == null ? null : usage.getPromptTokens().longValue();
            Long completion = usage.getCompletionTokens() == null ? null : usage.getCompletionTokens().longValue();
            control.recordUsage(prompt, completion);
        }
    }
}
