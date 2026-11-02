package org.etd.ai.agent.biz;

import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.config.AgentChatProperties;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.entity.AgentMessageEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.ai.agent.service.AgentRunService;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 租约持久化与过期记录修复；不调度任务，不获得用户会话或工具调用权限。 */
@Service
@RequiredArgsConstructor
public class AgentRunLeaseBizService implements AgentRunLeaseStore {
    private final AgentRunService runs;
    private final AgentMessageService messages;
    private final AgentChatProperties properties;

    /** @param runId 本轮标识 @return 数据库续租及取消状态，框架不自行推断数据库规则 */
    @Override
    public AgentRuntimeStatus renewRunLease(String runId) {
        Long id = Long.valueOf(runId);
        if (runs.renewLease(id, properties.getLeaseSeconds())) {
            return AgentRuntimeStatus.RUNNING;
        }
        return AgentRunBizService.resolveRuntimeStatus(runs.requireRun(id).getRunStatus());
    }

    /** 每批最多100个失联任务；仅跨租户领取语句忽略租户，其余更新按任务真实租户执行。 */
    @Override
    @Transactional
    public void finishExpiredRunList() {
        Long previous = RequestContext.getTenantCode();
        try {
            for (AgentRunEntity run : runs.fetchExpiredRunList()) {
                RequestContext.setTenantCode(run.getTenantId());
                AgentMessageEntity assistant = messages.fetchAssistant(run.getId());
                run.setRunStatus(AgentChatPolicy.FAILED);
                run.setErrorCode(AgentChatPolicy.LEASE_EXPIRED);
                run.setErrorMessage("执行节点失联或租约到期，请重新发送问题");
                String status = runs.finishRun(run);
                if (status != null && assistant != null) {
                    String messageStatus = AgentChatPolicy.CANCELLED.equals(status)
                            ? AgentChatPolicy.INTERRUPTED : AgentChatPolicy.FAILED;
                    messages.modifyAssistant(run.getId(), assistant.getContent(), messageStatus, assistant.getCitations());
                }
            }
        } finally {
            RequestContext.setTenantCode(previous);
        }
    }

}
