package org.etd.ai.agent.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.mapper.AgentConversationMapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.springframework.stereotype.Service;
import org.etd.framework.common.core.user.UserDetails;

/** 个人会话能力，所有者隐私与租户隔离同时生效，不使用Agent公开范围放行聊天内容。 */
@Service
@RequiredArgsConstructor
@DataPermission(enable = false)
public class AgentConversationService {
    private final AgentConversationMapper mapper;

    /** @return 已认证用户ID @throws ApiRuntimeException 无有效身份或租户 */
    public Long requireUserId() {
        UserDetails user = RequestContext.getUser();
        if (user == null || user.getId() == null || RequestContext.getTenantCode() == null) {
            throw new ApiRuntimeException("聊天需要有效登录身份及租户上下文");
        }
        return user.getId();
    }

    /** @param id 会话ID @param lock 是否在调用方短事务加锁 @return 当前用户会话 @throws ApiRuntimeException 无访问权 */
    public AgentConversationEntity requireConversation(Long id, boolean lock) {
        Long userId = requireUserId();
        Long ownerId = RequestContext.getUser().isPlatformAdmin() ? null : userId;
        AgentConversationEntity entity = mapper.selectOwned(id, ownerId, lock);
        if (entity == null) {
            throw new ApiRuntimeException("会话不存在或无访问权限");
        }
        return entity;
    }

    /** @param agentId 已授权Agent @param type 已校验类型 @param title 标题 @return 新会话 */
    public AgentConversationEntity createConversation(Long agentId, String type, String title) {
        AgentConversationEntity entity = new AgentConversationEntity();
        entity.setId(IdWorker.getId());
        entity.setAgentId(agentId);
        entity.setUserId(requireUserId());
        entity.setTenantId(RequestContext.getTenantCode());
        entity.setTitle(title.trim());
        entity.setConversationType(type);
        mapper.insertConversation(entity);
        return requireConversation(entity.getId(), false);
    }

    /** @param agentId 可选Agent @param beforeId 降序游标 @param size 最大条数 @return 个人会话列表 */
    public List<AgentConversationEntity> fetchConversationList(Long agentId, Long beforeId, int size) {
        Long userId = requireUserId();
        Long ownerId = RequestContext.getUser().isPlatformAdmin() ? null : userId;
        return mapper.selectOwnedList(ownerId, agentId, beforeId, size);
    }

    /** @param id 已授权会话ID，调用方持有会话行锁 */
    public void updateLastMessageTime(Long id) {
        mapper.updateLastMessageTime(id);
    }
}
