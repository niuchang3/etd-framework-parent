package org.etd.ai.agent.biz;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.config.AgentChatProperties;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.converter.AgentConverter;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.model.AgentChatExecution;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.service.AgentConversationService;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.ai.agent.service.AgentRunService;
import org.etd.ai.agent.service.AgentService;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.common.permission.constant.AgentPermissionCode;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.constants.PermissionCode;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.PermissionAuthority;
import org.etd.framework.common.core.user.UserDetails;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.etd.ai.agent.converter.AgentChatConverter;
import org.etd.framework.starter.ai.runtime.model.AgentChatCommand;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务会话规则和短事务准备：不组装模型、工具、执行对象或框架回调。
 */
@Service
@RequiredArgsConstructor
public class AgentChatBizService implements AgentChatPrepareService {
    private final AgentService agents;
    private final AgentConversationService conversations;
    private final AgentMessageService messages;
    private final AgentRunService runs;
    private final ModelConnectionService connections;
    private final KnowledgeModelSettingsService settings;
    private final AgentChatProperties properties;

    /** 框架在准入后通过 Spring 代理调用；原子创建 Run/消息并返回授权数据。 */
    @Override
    @Transactional
    public AgentChatPreparation prepare(AgentChatCommand command) {
        AgentChatExecution execution = beginRun(command.sessionId(), command.requestId(), command.question());
        return AgentChatConverter.INSTANCE.toPreparation(execution);
    }

    @Transactional
    public AgentConversationEntity createSession(Long agentId, String type, String title) {
        requireConfiguration(agentId, type);
        return conversations.createConversation(agentId, type, title);
    }

    @Transactional
    public void removeSession(Long sessionId) {
        conversations.requireConversation(sessionId, true);
        runs.requireNoActiveRun(sessionId);
        conversations.removeConversation(sessionId);
    }

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
