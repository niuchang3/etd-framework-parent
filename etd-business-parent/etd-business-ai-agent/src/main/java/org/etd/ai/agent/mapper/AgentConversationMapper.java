package org.etd.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.agent.entity.AgentConversationEntity;

/** 个人会话数据访问；所有者条件是业务隐私边界，不替代框架租户拦截。 */
public interface AgentConversationMapper extends BaseMapper<AgentConversationEntity> {
    /** @param entity 可信会话字段 @return 插入数 */
    int insertConversation(@Param("e") AgentConversationEntity entity);
    /** @param id 会话 @param userId 当前用户 @param lock 是否锁会话分配消息序号 @return 所有者会话 */
    AgentConversationEntity selectOwned(@Param("id") Long id, @Param("userId") Long userId, @Param("lock") boolean lock);
    /** @param userId 当前用户 @param agentId 可选Agent @param beforeId 向前翻页游标 @param size 条数 @return 会话列表 */
    List<AgentConversationEntity> selectOwnedList(@Param("userId") Long userId, @Param("agentId") Long agentId,
            @Param("beforeId") Long beforeId, @Param("size") int size);
    /** @param id 已授权且加锁的空闲会话 @return 逻辑删除条数，保留消息与运行记录 */
    int deleteConversation(@Param("id") Long id);
    /** @param id 已加锁会话 @return 修改条数 */
    int updateLastMessageTime(@Param("id") Long id);
}
