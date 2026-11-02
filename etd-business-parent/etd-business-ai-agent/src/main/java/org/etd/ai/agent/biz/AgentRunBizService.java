package org.etd.ai.agent.biz;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.config.AgentChatProperties;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.converter.AgentChatConverter;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.model.AgentCitation;
import org.etd.ai.agent.service.AgentRunService;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.ai.agent.service.AgentConversationService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业务实现框架运行生命周期插槽，编排个人会话授权、Run状态与消息落库事务。 */
@Service
@RequiredArgsConstructor
public class AgentRunBizService implements AgentRunStore {
    private final AgentRunService runs;
    private final AgentMessageService messages;
    private final AgentConversationService conversations;
    private final AgentChatProperties properties;

    /** @param progress 框架部分正文及引用 @throws ApiRuntimeException 无法继续保存时明确停止 */
    @Override
    @Transactional
    public void saveRunProgress(AgentRunProgress progress) {
        Long id = Long.valueOf(progress.runId());
        List<AgentCitation> citations = AgentChatConverter.INSTANCE.toCitationList(progress.references());
        if (!checkpointRun(id, progress.content(), citations)) {
            throw new ApiRuntimeException("运行租约已结束");
        }
    }

    /** @param result 框架期望终态 @return 数据库裁决后的实际终态及脱敏错误 */
    @Override
    @Transactional
    public AgentRuntimeResult finishRun(AgentRuntimeResult result) {
        AgentRunProgress progress = result.progress();
        Long id = Long.valueOf(progress.runId());
        AgentRunEntity run = runs.requireRun(id);
        run.setRunStatus(result.status().getCode());
        run.setInputTokens(progress.inputTokens());
        run.setOutputTokens(progress.outputTokens());
        run.setErrorCode(result.errorCode());
        run.setErrorMessage(result.errorMessage());
        List<AgentCitation> citations = AgentChatConverter.INSTANCE.toCitationList(progress.references());
        String status = finishRun(run, progress.content(), citations);
        AgentRunEntity persisted = runs.requireRun(id);
        AgentRuntimeStatus actualStatus = resolveRuntimeStatus(status);
        return new AgentRuntimeResult(progress, actualStatus, persisted.getErrorCode(), persisted.getErrorMessage());
    }

    static AgentRuntimeStatus resolveRuntimeStatus(String status) {
        return switch (status) {
            case AgentChatPolicy.RUNNING -> AgentRuntimeStatus.RUNNING;
            case AgentChatPolicy.CANCEL_REQUESTED, AgentChatPolicy.CANCELLED -> AgentRuntimeStatus.CANCELLED;
            case AgentChatPolicy.SUCCEEDED -> AgentRuntimeStatus.SUCCEEDED;
            case AgentChatPolicy.FAILED -> AgentRuntimeStatus.FAILED;
            default -> throw new ApiRuntimeException("无法识别的运行状态");
        };
    }

    /** @param result 期望终态 @param content 最后正文 @param citations 引用 @return 数据库实际终态 */
    @Transactional
    public String finishRun(AgentRunEntity result, String content, List<AgentCitation> citations) {
        String status = runs.finishRun(result);
        if (status == null) {
            return runs.requireRun(result.getId()).getRunStatus();
        }
        String messageStatus = AgentChatPolicy.SUCCEEDED.equals(status) ? AgentChatPolicy.COMPLETED
                : AgentChatPolicy.CANCELLED.equals(status) ? AgentChatPolicy.INTERRUPTED : AgentChatPolicy.FAILED;
        messages.modifyAssistant(result.getId(), content, messageStatus, citations);
        return status;
    }

    /** @param id 执行 @param content 部分正文 @param citations 引用 @return 是否仍持有运行租约 */
    @Transactional
    public boolean checkpointRun(Long id, String content, List<AgentCitation> citations) {
        if (!runs.renewLease(id, properties.getLeaseSeconds())) {
            return false;
        }
        messages.modifyAssistant(id, content, AgentChatPolicy.GENERATING, citations);
        return true;
    }

    /** @param id Run @return 经会话所有权校验的Run */
    public AgentRunEntity requireOwnedRun(Long id) {
        AgentRunEntity run = runs.requireRun(id);
        conversations.requireConversation(run.getConversationId(), false);
        return run;
    }

    /** @param sessionId 个人会话 @param requestId 原提交标识 @return 原执行记录 */
    public AgentRunEntity requireOwnedRunByRequest(Long sessionId, String requestId) {
        conversations.requireConversation(sessionId, false);
        return runs.requireRunByRequest(sessionId, requestId);
    }

    /** @param id Run @return 取消后的状态；已终态幂等返回 */
    @Transactional
    public AgentRunEntity cancelRun(Long id) {
        requireOwnedRun(id);
        return runs.requestCancel(id);
    }


}
