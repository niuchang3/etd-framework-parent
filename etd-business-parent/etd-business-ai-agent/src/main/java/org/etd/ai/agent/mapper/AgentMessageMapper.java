package org.etd.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.agent.entity.AgentMessageEntity;
import org.etd.ai.agent.model.AgentCitation;

/** 消息读写，只查询消息表；会话所有权由上层先行校验。 */
public interface AgentMessageMapper extends BaseMapper<AgentMessageEntity> {
    /** @param entity 可信消息 @return 插入数 */
    int insertMessage(@Param("e") AgentMessageEntity entity);
    /** @param conversationId 已锁会话 @return 当前最大序号 */
    long selectLastSequence(@Param("conversationId") Long conversationId);
    /** @param conversationId 已授权会话 @param beforeSequence 向前游标 @param size 最大条数 @return 按序号降序的历史 */
    List<AgentMessageEntity> selectMessageList(@Param("conversationId") Long conversationId,
            @Param("beforeSequence") Long beforeSequence, @Param("size") int size);
    /** @param runId 本轮执行 @return 该轮助手消息 */
    AgentMessageEntity selectAssistant(@Param("runId") Long runId);
    /** @param runId 本轮执行 @param content 当前部分正文 @param status 消息状态 @param citations 实际引用 @return 修改条数 */
    int updateAssistant(@Param("runId") Long runId, @Param("content") String content,
            @Param("status") String status, @Param("citations") List<AgentCitation> citations);
}
