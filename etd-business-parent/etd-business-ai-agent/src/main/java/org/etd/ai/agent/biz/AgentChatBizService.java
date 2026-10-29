package org.etd.ai.agent.biz;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.ai.chat.model.ChatModel;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.agent.config.AgentChatProperties;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.converter.AgentConverter;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.model.AgentChatExecution;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.service.AgentConversationService;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.ai.agent.service.AgentRunService;
import org.etd.ai.agent.service.AgentService;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.constants.PermissionCode;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.common.permission.constant.AgentPermissionCode;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.etd.ai.agent.converter.AgentChatConverter;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentExecutionRequest;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.runtime.spi.AgentRunEventListener;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.transaction.annotation.Transactional;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.model.AgentCitation;
import org.etd.ai.agent.model.AgentModelOptions;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.user.PermissionAuthority;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/** 聊天事务编排；所有外部模型和知识检索调用均在此类事务之外执行。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentChatBizService {
    /** 检索词重写只读取最近4条历史消息，保持当前上下文范围。 */
    private static final int REWRITE_HISTORY_MESSAGE_LIMIT = 4;
    /** 检索词重写的输出Token上限，不影响最终回答的输出预算。 */
    private static final int REWRITE_MAX_OUTPUT_TOKENS = 64;
    /** 重写任务沿用低随机性参数，减少追问检索词偏移。 */
    private static final double REWRITE_TEMPERATURE = 0.1;
    /** 只重写检索词，不让该模型调用提前回答用户问题。 */
    private static final String REWRITE_SYSTEM_PROMPT = "你是一个搜索意图提炼助手。请根据对话历史，将用户的最新追问重写为一个语义完整、独立可检索的搜索语句。"
            + "若最新追问表意已完整且无代词指代，直接原样输出。不要回答问题，严禁输出任何解释，仅输出重写后的语句。";
    private final AgentService agents;
    private final AgentConversationService conversations;
    private final AgentMessageService messages;
    private final AgentRunService runs;
    private final ModelConnectionService connections;
    private final KnowledgeModelSettingsService settings;
    private final AgentChatProperties properties;
    private final AgentRunRecordService runRecordService;
    private final AgentRuntimeProperties runtimeProperties;
    private final ModelClientRegistry clients;
    private final KnowledgeRecallBizService knowledge;

    /** 业务提供配置、授权模型与检索插槽；生命周期实现由Spring按接口注入，保证回调事务生效。
     * @param sessionId 会话 @param requestId 幂等ID @param question 问题
     * @param eventListener 输出插槽 @return 框架执行定义
     */
    @Transactional
    public AgentRunRequest prepareRun(Long sessionId, String requestId, String question,
            AgentRunEventListener eventListener) {
        AgentChatExecution execution = beginRun(sessionId, requestId, question.trim());
        Supplier<ChatModel> model = () -> clients.getChatModel(execution.modelTenantId(),
                execution.configuration().getModelConnectionId().toString());
        AgentExecutionRequest request = createExecutionRequest(execution, model);
        Map<String, Object> attributes = Map.of(AgentChatPolicy.SESSION_ID, execution.run().getConversationId(),
                AgentChatPolicy.MESSAGE_ID, execution.assistantMessageId());
        return new AgentRunRequest(execution.run().getId().toString(), request, model,
                runRecordService, eventListener, attributes);
    }

    private AgentExecutionRequest createExecutionRequest(AgentChatExecution execution,
            Supplier<ChatModel> modelSupplier) {
        AgentConfiguration config = execution.configuration();
        ChatOptions.Builder builder = ChatOptions.builder();
        AgentModelOptions options = config.getModelOptions();
        builder.maxTokens(options == null || options.getMaxTokens() == null
                ? runtimeProperties.getDefaultMaxOutputTokens() : options.getMaxTokens());
        if (options != null && options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        ChatOptions chatOptions = builder.build();
        AgentKnowledgeQueryService knowledgeService = query -> retrieveKnowledge(config.getKnowledgeBaseIds(),
                query, execution.history(), modelSupplier);
        boolean citations = !Boolean.FALSE.equals(config.getCitationEnabled());
        return new AgentExecutionRequest(config.getSystemPrompt(), execution.question(), execution.history(),
                chatOptions, knowledgeService, runtimeProperties.getInputTokenBudget(), runtimeProperties.getMaxReferences(), citations);
    }

    /** 业务实现检索插槽，负责多轮意图消解、知识库授权及来源转换；框架不读取知识库表或业务Entity。 */
    private List<List<KnowledgeFragment>> retrieveKnowledge(List<Long> ids, String question,
            List<Message> history, Supplier<ChatModel> modelSupplier) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String searchQuery = rewriteSearchQuery(question, history, modelSupplier);
        List<List<KnowledgeRecallHit>> groups = knowledge.recallEnabledList(ids, searchQuery);
        List<List<KnowledgeFragment>> result = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            List<KnowledgeFragment> fragments = new ArrayList<>();
            for (KnowledgeRecallHit hit : groups.get(index)) {
                AgentCitation citation = AgentChatConverter.INSTANCE.toCitation(hit, ids.get(index));
                Map<String, Object> metadata = Map.of(AgentChatPolicy.CITATION, citation);
                KnowledgeFragment fragment = new KnowledgeFragment(hit.getChunkId().toString(), hit.getContent(), metadata);
                fragments.add(fragment);
            }
            result.add(fragments);
        }
        return result;
    }

    /** 存在历史时尝试重写追问，失败或无有效结果时沿用原问题；不改变当前检索策略。 */
    private String rewriteSearchQuery(String question, List<Message> history, Supplier<ChatModel> modelSupplier) {
        if (history == null || history.isEmpty()) {
            return question;
        }
        try {
            ChatModel model = modelSupplier.get();
            if (model == null) {
                return question;
            }
            Prompt prompt = createSearchRewritePrompt(question, history);
            ChatResponse response = model.call(prompt);
            return readSearchRewriteResult(response, question);
        } catch (Exception exception) {
            // 保留原有失败处理，但记录异常类型便于排查；不输出用户历史、问题或模型异常正文。
            log.warn("Agent检索词重写失败，沿用原问题，异常类型={}", exception.getClass().getSimpleName());
            return question;
        }
    }

    /** 系统指令、最近历史和本轮问题按原顺序组装，参数仅作用于本次检索词重写。 */
    private Prompt createSearchRewritePrompt(String question, List<Message> history) {
        List<Message> messages = new ArrayList<>();
        SystemMessage instruction = new SystemMessage(REWRITE_SYSTEM_PROMPT);
        messages.add(instruction);
        int start = Math.max(0, history.size() - REWRITE_HISTORY_MESSAGE_LIMIT);
        for (int index = start; index < history.size(); index++) {
            messages.add(history.get(index));
        }
        UserMessage latestQuestion = new UserMessage("最新追问：" + question);
        messages.add(latestQuestion);
        ChatOptions options = ChatOptions.builder().maxTokens(REWRITE_MAX_OUTPUT_TOKENS)
                .temperature(REWRITE_TEMPERATURE).build();
        return new Prompt(messages, options);
    }

    /** 空响应、缺少正文或空白正文均沿用原问题，有效结果只去除首尾空白。 */
    private String readSearchRewriteResult(ChatResponse response, String question) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return question;
        }
        String rewritten = response.getResult().getOutput().getText();
        if (rewritten == null || rewritten.isBlank()) {
            return question;
        }
        return rewritten.trim();
    }

    /** @param agentId Agent @param type 会话类型 @param title 标题 @return 已授权会话 */
    @Transactional
    public AgentConversationEntity createSession(Long agentId, String type, String title) {
        requireConfiguration(agentId, type);
        return conversations.createConversation(agentId, type, title);
    }

    /** 逻辑删除会话，消息和运行记录保留，但所有前端访问仍须校验会话未删除。
     * @param sessionId 会话ID @throws ApiRuntimeException 会话不可访问或生成尚未结束
     */
    @Transactional
    public void removeSession(Long sessionId) {
        // 与开始聊天持有同一行锁，避免校验空闲后又插入新的运行记录。
        conversations.requireConversation(sessionId, true);
        runs.requireNoActiveRun(sessionId);
        conversations.removeConversation(sessionId);
    }

    /** @param sessionId 会话 @param requestId 幂等ID @param question 问题 @return 固定本轮配置及历史 @throws ApiRuntimeException 权限或执行状态不满足 */
    @Transactional
    public AgentChatExecution beginRun(Long sessionId, String requestId, String question) {
        AgentConversationEntity session = conversations.requireConversation(sessionId, true);
        runs.requireIdle(sessionId, requestId);
        AgentConfiguration config = requireConfiguration(session.getAgentId(), session.getConversationType());
        settings.validate(config.getModelConnectionId(), ModelType.CHAT, null);
        ModelConnectionEntity connection = connections.requireConnection(config.getModelConnectionId(), false);
        int historyLimit = config.getHistoryMessageLimit() == null
                ? AgentChatPolicy.DEFAULT_HISTORY_LIMIT : config.getHistoryMessageLimit();
        List<Message> history = messages.fetchHistoryList(sessionId, historyLimit);
        AgentRunEntity run = runs.createRun(session, requestId, config.getModelConnectionId(), properties.getLeaseSeconds());
        Long messageId = messages.createMessagePair(session, run.getId(), question);
        conversations.updateLastMessageTime(sessionId);
        return new AgentChatExecution(run, messageId, config, history, question, connection.getTenantId().toString());
    }

    /** 调试既要求菜单写权限，也由AgentService核对数据维护范围；不能仅凭会话类型使用草稿。 */
    private void requireDebugPermission() {
        UserDetails user = RequestContext.getUser();
        if (user == null || user.isReadOnly()) {
            throw new ApiRuntimeException("无Agent调试权限");
        }
        if (user.isPlatformAdmin()) {
            return;
        }
        String authority = PermissionCode.createAuthority(AgentPermissionCode.AGENT, PermissionAction.WRITE);
        if (user.getAuthorities() != null) {
            for (PermissionAuthority permission : user.getAuthorities()) {
                if (authority.equals(permission.getAuthority())) {
                    return;
                }
            }
        }
        throw new ApiRuntimeException("调试需要Agent菜单写权限");
    }

    /** @param id Agent ID @param type chat正式或debug调试 @return 已授权的本轮配置副本，不得直接对外序列化 */
    public AgentConfiguration requireConfiguration(Long id, String type) {
        if (!AgentChatPolicy.CHAT.equals(type) && !AgentChatPolicy.DEBUG.equals(type)) {
            throw new ApiRuntimeException("不支持的会话类型");
        }
        boolean debug = AgentChatPolicy.DEBUG.equals(type);
        if (debug) {
            requireDebugPermission();
        }
        AgentEntity agent = agents.requireAgent(id, debug, false);
        if (!Objects.equals(agent.getDataStatus(), BasicConstant.DataStatus.ENABLED_CODE)) {
            throw new ApiRuntimeException("Agent已停用");
        }
        AgentConfiguration source = debug ? agent.getDraftConfig() : agent.getPublishedConfig();
        if (source == null || source.getModelConnectionId() == null || source.getSystemPrompt() == null
                || source.getSystemPrompt().isBlank()) {
            throw new ApiRuntimeException("Agent尚未发布或运行配置不完整");
        }
        return AgentConverter.INSTANCE.copyConfiguration(source);
    }
}
