package org.etd.framework.starter.ai.memory.semantic;

import java.util.List;
import org.etd.framework.starter.ai.memory.working.AgentWorkingMemory;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;

/** 复用官方追问压缩器，保留最近四条历史、低随机性和失败沿用原问题的既有检索策略。 */
public final class AgentSearchQueryRewriter {
    private static final Logger LOG = LoggerFactory.getLogger(AgentSearchQueryRewriter.class);
    private static final int HISTORY_LIMIT = 4;
    private static final int MAX_OUTPUT_TOKENS = 64;
    private static final double TEMPERATURE = 0.1;
    private static final String INSTRUCTION = "你是一个搜索意图提炼助手。请根据对话历史，将用户的最新追问重写为一个语义完整、独立可检索的搜索语句。"
            + "若最新追问表意已完整且无代词指代，直接原样输出。不要回答问题，严禁输出任何解释，仅输出重写后的语句。";

    /**
     * @param question 当前提问 @param history 已授权历史 @param model 已授权官方客户端
     * @param control 当前 Run 的取消及用量 @param budget 同一 Agent 的输入 Token 硬预算
     * @return 检索词；重写失败按已有策略沿用原问题，但取消必须传播
     */
    public String rewrite(String question, List<Message> history, ChatModel model, AgentRunControl control, int budget) {
        if (history.isEmpty()) {
            return question;
        }
        control.requireActive();
        try {
            CompressionQueryTransformer transformer = createTransformer(model, control, budget);
            int start = Math.max(0, history.size() - HISTORY_LIMIT);
            List<Message> recent = List.copyOf(history.subList(start, history.size()));
            Query query = Query.builder().text(question).history(recent).build();
            Query rewritten = transformer.transform(query);
            control.requireActive();
            return rewritten.text().trim();
        } catch (RuntimeException failure) {
            control.requireActive();
            // 这是保留的已确认失败策略；只记类型，不输出问题、历史或模型的原始异常正文。
            LOG.warn("Agent 检索词重写失败，沿用原问题，异常类型={}", failure.getClass().getSimpleName());
            return question;
        }
    }

    private CompressionQueryTransformer createTransformer(ChatModel model, AgentRunControl control, int budget) {
        ChatOptions options = ChatOptions.builder().maxTokens(MAX_OUTPUT_TOKENS).temperature(TEMPERATURE).build();
        RequestContextSnapshot snapshot = RequestContextSnapshot.capture();
        RewriteAdvisor advisor = new RewriteAdvisor(snapshot, control, budget);
        ChatClient.Builder client = ChatClient.builder(model).defaultOptions(options).defaultSystem(INSTRUCTION)
                .defaultAdvisors(advisor);
        PromptTemplate template = new PromptTemplate("对话历史：\n{history}\n最新追问：{query}");
        return CompressionQueryTransformer.builder().chatClientBuilder(client).promptTemplate(template).build();
    }

    /** 官方同步 ChatClient 扩展点保障辅助调用；不再包装 ChatModel 或自行格式化历史。 */
    private record RewriteAdvisor(RequestContextSnapshot context, AgentRunControl control, int budget) implements CallAdvisor {
        @Override
        public String getName() {
            return "agent_query_rewrite_guard";
        }

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            AgentWorkingMemory memory = new AgentWorkingMemory();
            if (memory.countMessages(request.prompt().getInstructions()) > budget) {
                throw new IllegalArgumentException("检索词重写输入超出 Token 预算");
            }
            control.requireActive();
            control.nextCallRound();
            ChatClientResponse response;
            try (Scope scope = context.openScope()) {
                response = chain.nextCall(request);
            }
            recordUsage(response.chatResponse());
            control.requireActive();
            return response;
        }

        private void recordUsage(ChatResponse response) {
            if (response != null && response.getMetadata().getUsage() != null) {
                Usage usage = response.getMetadata().getUsage();
                Long input = usage.getPromptTokens() == null ? null : usage.getPromptTokens().longValue();
                Long output = usage.getCompletionTokens() == null ? null : usage.getCompletionTokens().longValue();
                control.recordUsage(input, output);
            }
        }
    }
}
