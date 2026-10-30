package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.function.Consumer;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.model.AgentExecutionRequest;
import org.etd.framework.starter.ai.runtime.model.AgentModelInput;
import org.springframework.ai.chat.messages.Message;

/** 每轮独立的官方模型前 Hook；在请求上下文中检索并按现有预算替换模型输入。 */
@HookPositions({HookPosition.BEFORE_MODEL})
final class AgentKnowledgeHook extends MessagesModelHook {
    private final AgentKnowledgePromptBuilder promptBuilder = new AgentKnowledgePromptBuilder();
    private final AgentExecutionRequest request;
    private final Consumer<AgentModelInput> prepared;
    private final RequestContextSnapshot context;

    AgentKnowledgeHook(AgentExecutionRequest request,
            Consumer<AgentModelInput> prepared, RequestContextSnapshot context) {
        this.request = request;
        this.prepared = prepared;
        this.context = context;
    }

    @Override
    public String getName() {
        return "authorized_knowledge";
    }

    /** 图调度线程不自动继承身份；整个检索及引用发布过程使用本轮快照，退出后恢复。 */
    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        return context.call(this::prepareModelInput);
    }

    private AgentCommand prepareModelInput() {
        AgentModelInput input = promptBuilder.prepare(request);
        prepared.accept(input);
        List<Message> messages = input.prompt().getInstructions();
        return new AgentCommand(messages);
    }
}
