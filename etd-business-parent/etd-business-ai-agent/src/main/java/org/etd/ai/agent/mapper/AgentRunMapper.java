package org.etd.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.agent.entity.AgentRunEntity;

/** Run幂等与状态机；条件更新决定取消、完成及租约回收的唯一赢家。 */
public interface AgentRunMapper extends BaseMapper<AgentRunEntity> {
    /** @param entity 可信运行记录 @return 插入数 */
    int insertRun(@Param("e") AgentRunEntity entity);
    /** @param id Run ID @return 当前状态 */
    AgentRunEntity selectRun(@Param("id") Long id);
    /** @param conversationId 会话 @param requestId 幂等标识 @return 已有执行 */
    AgentRunEntity selectByRequest(@Param("conversationId") Long conversationId, @Param("requestId") String requestId);
    /** @param conversationId 会话 @return 活跃执行 */
    AgentRunEntity selectActive(@Param("conversationId") Long conversationId);
    /** @param id Run @return 请求取消的修改数 */
    int requestCancel(@Param("id") Long id);
    /** @param id Run @param leaseUntil 新租约 @return 成功续租数，过期任务不能复活 */
    int renewLease(@Param("id") Long id, @Param("leaseUntil") Instant leaseUntil);
    /** @param e 期望终态及用量 @return 实际终态，取消请求先到则取消优先，已终态返回空 */
    String finishRun(@Param("e") AgentRunEntity e);
    /** 后台回收跨租户失联任务，不代表用户查询绕过租户；仅此维护语句忽略租户拦截。
     * @return 锁定的过期Run，事务中处理，最多100条 */
    @InterceptorIgnore(tenantLine = "true")
    List<AgentRunEntity> selectExpiredList();
}
