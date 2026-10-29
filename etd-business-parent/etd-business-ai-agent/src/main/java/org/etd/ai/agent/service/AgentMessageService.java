package org.etd.ai.agent.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.entity.AgentMessageEntity;
import org.etd.ai.agent.mapper.AgentMessageMapper;
import org.etd.ai.agent.model.AgentCitation;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/** 消息能力；调用方负责先校验个人会话归属，不直接组合其他Service。 */
@Service
@RequiredArgsConstructor
@DataPermission(enable = false)
public class AgentMessageService {
    private final AgentMessageMapper mapper;

    /** @param conversation 已加锁会话 @param runId 本轮ID @param content 问题 @return 空助手消息ID */
    public Long createMessagePair(AgentConversationEntity conversation, Long runId, String content) {
        long sequence = mapper.selectLastSequence(conversation.getId());
        AgentMessageEntity user = createMessage(conversation, runId, sequence + 1, AgentChatPolicy.USER, content);
        user.setMessageStatus(AgentChatPolicy.COMPLETED);
        mapper.insertMessage(user);
        AgentMessageEntity assistant = createMessage(conversation, runId, sequence + 2, AgentChatPolicy.ASSISTANT, "");
        assistant.setMessageStatus(AgentChatPolicy.GENERATING);
        mapper.insertMessage(assistant);
        return assistant.getId();
    }

    /** @param conversationId 已授权会话 @param beforeSequence 向前游标 @param size 条数 @return 按序号升序的一页消息 */
    public List<AgentMessageEntity> fetchMessageList(Long conversationId, Long beforeSequence, int size) {
        List<AgentMessageEntity> rows = mapper.selectMessageList(conversationId, beforeSequence, size);
        Collections.reverse(rows);
        return rows;
    }

    /** @param conversationId 已授权会话 @param limit 消息上限 @return 过滤失败记录后的升序历史消息 */
    public List<Message> fetchHistoryList(Long conversationId, int limit) {
        if (limit < 1) {
            return List.of();
        }
        List<AgentMessageEntity> rows = fetchMessageList(conversationId, null, limit);
        List<Message> history = new ArrayList<>();
        for (AgentMessageEntity row : rows) {
            if (AgentChatPolicy.FAILED.equals(row.getMessageStatus())) {
                continue;
            }
            String content = row.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            if (AgentChatPolicy.USER.equals(row.getRole())) {
                history.add(new UserMessage(content));
            } else if (AgentChatPolicy.ASSISTANT.equals(row.getRole())) {
                String text = AgentChatPolicy.INTERRUPTED.equals(row.getMessageStatus())
                        ? content + "\n[用户已中断此轮生成]" : content;
                history.add(new AssistantMessage(text));
            }
        }
        return history;
    }

    /** @param runId 已授权运行 @return 助手消息 */
    public AgentMessageEntity fetchAssistant(Long runId) {
        return mapper.selectAssistant(runId);
    }

    /** @param runId 已授权运行 @param content 部分或完整正文 @param status 目标状态 @param citations 引用 */
    public void modifyAssistant(Long runId, String content, String status, List<AgentCitation> citations) {
        mapper.updateAssistant(runId, content, status, citations);
    }

    private AgentMessageEntity createMessage(AgentConversationEntity conversation, Long runId,
            long sequence, String role, String content) {
        AgentMessageEntity entity = new AgentMessageEntity();
        entity.setId(IdWorker.getId());
        entity.setTenantId(conversation.getTenantId());
        entity.setConversationId(conversation.getId());
        entity.setRunId(runId);
        entity.setSequenceNo(sequence);
        entity.setRole(role);
        entity.setContent(content);
        return entity;
    }
}
