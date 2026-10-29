package org.etd.ai.agent.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.mapper.AgentRunMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.springframework.stereotype.Service;

/** Run幂等、续租与条件终态能力；事务和消息联动由Biz编排。 */
@Service
@RequiredArgsConstructor
@DataPermission(enable = false)
public class AgentRunService {
    private final AgentRunMapper mapper;

    /** @param id 运行ID @return 运行 @throws ApiRuntimeException 不可访问 */
    public AgentRunEntity requireRun(Long id) {
        AgentRunEntity entity = mapper.selectRun(id);
        if (entity == null) {
            throw new ApiRuntimeException("执行记录不存在或已清理");
        }
        return entity;
    }

    /** @param conversationId 已授权会话 @param requestId 幂等标识 @return 原执行 @throws ApiRuntimeException 标识不存在 */
    public AgentRunEntity requireRunByRequest(Long conversationId, String requestId) {
        AgentRunEntity run = mapper.selectByRequest(conversationId, requestId);
        if (run == null) {
            throw new ApiRuntimeException("请求尚未建立执行记录或已清理");
        }
        return run;
    }

    /** @param conversationId 已加锁会话 @param requestId 幂等ID @throws ApiRuntimeException 重复请求或会话忙 */
    public void requireIdle(Long conversationId, String requestId) {
        AgentRunEntity existing = mapper.selectByRequest(conversationId, requestId);
        if (existing != null) {
            throw new ApiRuntimeException("请求已提交，runId=" + existing.getId() + "，请查询运行状态和消息");
        }
        requireNoActiveRun(conversationId);
    }

    /** @param conversationId 已加锁的会话ID @throws ApiRuntimeException 正在生成或等待取消完成 */
    public void requireNoActiveRun(Long conversationId) {
        if (mapper.selectActive(conversationId) != null) {
            throw new ApiRuntimeException("当前会话正在生成，请先停止或等待完成");
        }
    }

    /** @param conversation 已锁会话 @param requestId 幂等ID @param connectionId 聊天连接 @param leaseSeconds 租约秒数 @return 新Run */
    public AgentRunEntity createRun(AgentConversationEntity conversation, String requestId, Long connectionId, int leaseSeconds) {
        AgentRunEntity run = new AgentRunEntity();
        run.setId(IdWorker.getId());
        run.setTenantId(conversation.getTenantId());
        run.setConversationId(conversation.getId());
        run.setRequestId(requestId);
        run.setModelConnectionId(connectionId);
        run.setRunStatus(AgentChatPolicy.RUNNING);
        Instant leaseUntil = Instant.now().plusSeconds(leaseSeconds);
        run.setLeaseUntil(leaseUntil);
        mapper.insertRun(run);
        return requireRun(run.getId());
    }

    /** @param id 已授权Run @return 请求后的当前状态 */
    public AgentRunEntity requestCancel(Long id) {
        mapper.requestCancel(id);
        return requireRun(id);
    }

    /** @param id 本节点Run @param leaseSeconds 租约秒数 @return 是否仍正常运行，过期不复活 */
    public boolean renewLease(Long id, int leaseSeconds) {
        Instant until = Instant.now().plusSeconds(leaseSeconds);
        return mapper.renewLease(id, until) == 1;
    }

    /** @param result 期望终态及用量 @return 实际新终态，已终态为空 */
    public String finishRun(AgentRunEntity result) {
        return mapper.finishRun(result);
    }

    /** @return 回收事务锁定的过期Run，不用于用户查询 */
    public List<AgentRunEntity> fetchExpiredRunList() {
        return mapper.selectExpiredList();
    }
}
