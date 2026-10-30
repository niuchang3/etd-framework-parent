package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;

/** 框架内部的追问检索词重写步骤；业务检索接口只接收最终检索词，不参与模型编排。 */
@Slf4j
final class AgentSearchQueryRewriter {
    /** 检索词重写只读取最近4条历史消息，保持当前上下文范围。 */
    private static final int REWRITE_HISTORY_MESSAGE_LIMIT = 4;
    /** 检索词重写的输出Token上限，不影响最终回答的输出预算。 */
    private static final int REWRITE_MAX_OUTPUT_TOKENS = 64;
    /** 重写任务沿用低随机性参数，减少追问检索词偏移。 */
    private static final double REWRITE_TEMPERATURE = 0.1;
    /** 只重写检索词，不让该模型调用提前回答用户问题。 */
    private static final String REWRITE_SYSTEM_PROMPT = "你是一个搜索意图提炼助手。请根据对话历史，将用户的最新追问重写为一个语义完整、独立可检索的搜索语句。"
            + "若最新追问表意已完整且无代词指代，直接原样输出。不要回答问题，严禁输出任何解释，仅输出重写后的语句。";

    /** 存在历史时尝试重写追问，失败或无有效结果时沿用原问题；不改变当前检索策略。 */
    String rewriteSearchQuery(String question, List<Message> history, Supplier<ChatModel> modelSupplier) {
        if (history == null || history.isEmpty()) {
            return question;
        }
        try {
            ChatModel model = modelSupplier.get();
            if (model == null) {
                return question;
            }
            CompressionQueryTransformer transformer = createTransformer(model);
            int start = Math.max(0, history.size() - REWRITE_HISTORY_MESSAGE_LIMIT);
            List<Message> recentHistory = List.copyOf(history.subList(start, history.size()));
            Query query = Query.builder().text(question).history(recentHistory).build();
            Query rewritten = transformer.transform(query);
            return rewritten.text().trim();
        } catch (Exception exception) {
            // 保留原有失败处理，但记录异常类型便于排查；不输出用户历史、问题或模型异常正文。
            log.warn("Agent检索词重写失败，沿用原问题，异常类型={}", exception.getClass().getSimpleName());
            return question;
        }
    }

    /** 官方压缩器负责历史格式化和模型调用，框架仅配置中文规则及本轮生成预算。 */
    private CompressionQueryTransformer createTransformer(ChatModel model) {
        ChatOptions options = ChatOptions.builder().maxTokens(REWRITE_MAX_OUTPUT_TOKENS)
                .temperature(REWRITE_TEMPERATURE).build();
        ChatClient.Builder client = ChatClient.builder(model).defaultOptions(options)
                .defaultSystem(REWRITE_SYSTEM_PROMPT);
        PromptTemplate template = new PromptTemplate("对话历史：\n{history}\n最新追问：{query}");
        return CompressionQueryTransformer.builder().chatClientBuilder(client).promptTemplate(template).build();
    }
}
