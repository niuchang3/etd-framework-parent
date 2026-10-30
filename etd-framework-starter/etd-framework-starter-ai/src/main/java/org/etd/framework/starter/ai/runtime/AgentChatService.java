package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.model.AgentChatRequest;
import org.etd.framework.starter.ai.runtime.model.AgentExecutionRequest;
import org.etd.framework.starter.ai.runtime.model.AgentKnowledgeQuery;
import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.runtime.spi.AgentRunEventListener;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.util.Assert;

/** 统一聊天入口；业务注册准备、检索、记录三个接口后，只需提交参数与输出接收器。 */
@RequiredArgsConstructor
public class AgentChatService {
    /** 为终态保存和输出关闭预留的时间，不让HTTP通道早于框架运行超时关闭。 */
    private static final long OUTPUT_TIMEOUT_GRACE_SECONDS = 10;
    private final AgentRuntime runtime;
    private final AgentRuntimeProperties properties;
    private final ModelClientRegistry models;
    private final AgentChatPrepareService preparation;
    private final AgentKnowledgeQueryService knowledge;
    private final AgentRunRecordService records;
    /** 重写、模型解析及内部回调连接均属于框架，不暴露给业务组装。 */
    private final AgentSearchQueryRewriter queryRewriter = new AgentSearchQueryRewriter();

    /** @return 传输输出通道的超时毫秒数，HTTP适配器无需了解内部运行超时和预留时间 */
    public long getOutputTimeoutMillis() {
        return TimeUnit.SECONDS.toMillis(properties.getTimeoutSeconds() + OUTPUT_TIMEOUT_GRACE_SECONDS);
    }

    /** 先检查容量，再调用准备接口；准备事务提交后启动异步执行，并自动绑定输出取消入口。
     * @param request 会话、幂等标识及用户问题 @param output 输出接收器，例如SSE适配器
     * @return 本轮控制器，非HTTP调用方可主动取消；调用方不负责管理工作线程或保存终态
     * @throws RuntimeException 容量不足、准备失败或输出绑定失败；异步失败通过记录接口和输出事件通知
     */
    public AgentRunControl start(AgentChatRequest request, AgentRunEventListener output) {
        Assert.notNull(request, "聊天请求不能为空");
        Assert.notNull(output, "聊天输出接收器不能为空");
        Supplier<AgentRunRequest> prepare = () -> prepareRun(request, output);
        AgentRunControl control = runtime.start(prepare);
        try {
            output.bind(control);
            return control;
        } catch (RuntimeException exception) {
            control.cancel();
            throw exception;
        }
    }

    /** 业务只返回数据；准备事务提交后的组装失败由框架补记终态，避免留下只能等待租约回收的运行。 */
    private AgentRunRequest prepareRun(AgentChatRequest request, AgentRunEventListener output) {
        AgentChatPreparation data = preparation.prepareChat(request);
        Assert.notNull(data, "聊天准备接口未返回数据");
        try {
            validatePreparation(data);
            return createRunRequest(request, data, output);
        } catch (RuntimeException exception) {
            if (data.getExecutionId() != null && !data.getExecutionId().isBlank()) {
                finishPreparationFailure(data.getExecutionId());
            }
            output.close();
            throw exception;
        }
    }

    /** 检查业务契约必填项，不在接口层构造模型或占用长事务。 */
    private void validatePreparation(AgentChatPreparation data) {
        Assert.hasText(data.getExecutionId(), "聊天准备数据缺少执行标识");
        Assert.hasText(data.getModelTenantId(), "聊天准备数据缺少模型租户");
        Assert.hasText(data.getModelConnectionId(), "聊天准备数据缺少模型连接");
        Assert.hasText(data.getSystemPrompt(), "聊天准备数据缺少系统提示词");
    }

    /** 立即复制集合，异步运行只持有本轮输入；模型实例延迟到准备事务之外取得。 */
    private AgentRunRequest createRunRequest(AgentChatRequest request, AgentChatPreparation data,
            AgentRunEventListener output) {
        List<Message> history = List.of();
        if (data.getHistory() != null) {
            history = List.copyOf(data.getHistory());
        }
        List<Long> baseIds = List.of();
        if (data.getKnowledgeBaseIds() != null) {
            baseIds = List.copyOf(data.getKnowledgeBaseIds());
        }
        String tenantId = data.getModelTenantId();
        String connectionId = data.getModelConnectionId();
        Supplier<ChatModel> model = () -> models.getChatModel(tenantId, connectionId);
        Supplier<List<List<KnowledgeFragment>>> retrieval = createKnowledgeSupplier(request.question(), history, baseIds, model);
        ChatOptions options = createChatOptions(data);
        AgentExecutionRequest execution = new AgentExecutionRequest(data.getSystemPrompt(), request.question(), history,
                options, retrieval, properties.getInputTokenBudget(), properties.getMaxReferences(), data.isCitationsEnabled());
        Map<String, Object> attributes = data.getAttributes();
        return new AgentRunRequest(data.getExecutionId(), execution, model, records, output, attributes);
    }

    /** 构造内部检索调用，业务不需要在配置中携带可执行回调。 */
    private Supplier<List<List<KnowledgeFragment>>> createKnowledgeSupplier(String question, List<Message> history,
            List<Long> baseIds, Supplier<ChatModel> model) {
        return () -> retrieveKnowledge(question, history, baseIds, model);
    }

    /** 无知识库不发起重写；有历史时复用当前授权模型重写检索词，再调用业务检索接口。 */
    private List<List<KnowledgeFragment>> retrieveKnowledge(String question, List<Message> history,
            List<Long> baseIds, Supplier<ChatModel> model) {
        if (baseIds.isEmpty()) {
            return List.of();
        }
        String query = queryRewriter.rewriteSearchQuery(question, history, model);
        AgentKnowledgeQuery request = new AgentKnowledgeQuery(baseIds, query);
        return knowledge.queryKnowledgeList(request);
    }

    /** 未配置的输出上限由框架统一提供，业务只返回已发布配置中的可选值。 */
    private ChatOptions createChatOptions(AgentChatPreparation data) {
        int maximum = properties.getDefaultMaxOutputTokens();
        if (data.getMaxOutputTokens() != null) {
            maximum = data.getMaxOutputTokens();
        }
        ChatOptions.Builder options = ChatOptions.builder().maxTokens(maximum);
        if (data.getTemperature() != null) {
            options.temperature(data.getTemperature());
        }
        return options.build();
    }

    /** 组装失败也通过同一记录接口结束运行，返回统一脱敏错误。 */
    private void finishPreparationFailure(String executionId) {
        AgentRunProgress progress = new AgentRunProgress(executionId, "", List.of(), null, null);
        AgentRuntimeResult result = new AgentRuntimeResult(progress, AgentRuntimeStatus.FAILED,
                AgentRuntime.EXECUTION_FAILED, "聊天准备数据无效，无法开始生成");
        records.finishRun(result);
    }
}
