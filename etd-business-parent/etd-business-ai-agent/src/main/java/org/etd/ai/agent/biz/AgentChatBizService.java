package org.etd.ai.agent.biz;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.ai.chat.model.ChatModel;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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
import org.springframework.ai.chat.messages.Message;

/** 聊天事务编排；所有外部模型和知识检索调用均在此类事务之外执行。 */
@Service
@RequiredArgsConstructor
public class AgentChatBizService {
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
        AgentExecutionRequest request = createExecutionRequest(execution);
        Map<String, Object> attributes = Map.of(AgentChatPolicy.SESSION_ID, execution.run().getConversationId(),
                AgentChatPolicy.MESSAGE_ID, execution.assistantMessageId());
        Supplier<ChatModel> model = () -> clients.getChatModel(execution.modelTenantId(),
                execution.configuration().getModelConnectionId().toString());
        return new AgentRunRequest(execution.run().getId().toString(), request, model,
                runRecordService, eventListener, attributes);
    }

    private AgentExecutionRequest createExecutionRequest(AgentChatExecution execution) {
        AgentConfiguration config = execution.configuration();
        ChatOptions.Builder builder = ChatOptions.builder();
        AgentModelOptions options = config.getModelOptions();
        builder.maxTokens(options == null || options.getMaxTokens() == null
                ? runtimeProperties.getDefaultMaxOutputTokens() : options.getMaxTokens());
        if (options != null && options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        ChatOptions chatOptions = builder.build();
        AgentKnowledgeQueryService knowledgeService = query -> retrieveKnowledge(config.getKnowledgeBaseIds(), query);
        boolean citations = !Boolean.FALSE.equals(config.getCitationEnabled());
        return new AgentExecutionRequest(config.getSystemPrompt(), execution.question(), execution.history(),
                chatOptions, knowledgeService, runtimeProperties.getInputTokenBudget(), runtimeProperties.getMaxReferences(), citations);
    }

    /** 业务实现检索插槽，负责知识库授权及来源转换；框架不读取知识库表或业务Entity。 */
    private List<List<KnowledgeFragment>> retrieveKnowledge(List<Long> ids, String question) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<List<KnowledgeRecallHit>> groups = knowledge.recallEnabledList(ids, question);
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
        if (user.getAuthorities() != null
                && user.getAuthorities().stream().anyMatch(value -> authority.equals(value.getAuthority()))) {
            return;
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
