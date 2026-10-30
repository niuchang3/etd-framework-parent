package org.etd.framework.starter.ai.runtime;

import java.util.function.Consumer;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.model.AgentExecutionRequest;
import org.etd.framework.starter.ai.runtime.model.AgentModelInput;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

/** 官方 ReactAgent 执行入口；知识准备通过模型前 Hook 接入，业务无需组装执行流程。 */
public class AgentExecutionEngine {
    /**
     * 创建本轮独立的官方 Agent；不配置检查点存储，历史及运行记录仍由业务数据库统一管理。
     * @param model 当前租户已授权模型
     * @param request 本轮问题、历史及检索策略
     * @param prepared 在模型调用前交付实际引用，失败直接终止本轮
     * @param responseObserver 读取原始响应统计，不写入正文
     * @return 官方图执行产生的模型增量，不重复发送最终聚合正文
     */
    public Flux<ChatResponse> stream(ChatModel model, AgentExecutionRequest request,
            Consumer<AgentModelInput> prepared, Consumer<ChatResponse> responseObserver) {
        RequestContextSnapshot context = RequestContextSnapshot.capture();
        AgentKnowledgeHook hook = new AgentKnowledgeHook(request, prepared, context);
        AgentContextChatModel contextualModel = new AgentContextChatModel(model, context, responseObserver);
        // Alibaba 1.1.2.0 仅接受工具选项类型；显式复制生成参数，避免普通 ChatOptions 被忽略。
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .maxTokens(request.options().getMaxTokens()).temperature(request.options().getTemperature())
                .internalToolExecutionEnabled(false).build();
        ReactAgent agent = ReactAgent.builder().name("knowledge_chat").model(contextualModel)
                .chatOptions(options).hooks(hook).enableLogging(false).build();
        return Flux.defer(() -> openAgentStream(agent, request.question()));
    }

    /** 图只负责执行，协议层只消费模型原始片段；图节点状态和最终聚合消息不会作为 delta 再次发送。 */
    private Flux<ChatResponse> openAgentStream(ReactAgent agent, String question) {
        try {
            Flux<NodeOutput> outputs = agent.stream(question);
            return outputs.handle(this::forwardModelResponse);
        } catch (Exception exception) {
            return Flux.error(exception);
        }
    }

    private void forwardModelResponse(NodeOutput output, SynchronousSink<ChatResponse> sink) {
        if (output instanceof StreamingOutput<?> streaming
                && streaming.getOutputType() == OutputType.AGENT_MODEL_STREAMING
                && streaming.getOriginData() instanceof ChatResponse response) {
            sink.next(response);
        }
    }

}
