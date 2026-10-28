package org.etd.framework.starter.ai.runtime.spi;

import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;

/** Agent运行记录服务插槽；业务负责续租、保存回答进度、结束执行和清理过期任务，框架不访问业务表。 */
public interface AgentRunRecordService {
    /** @param executionId 已授权执行标识 @return 续租后的状态，取消或失联时停止执行 */
    AgentRuntimeStatus renewRunLease(String executionId);
    /** @param progress 当前回答正文及引用资料，业务在短事务中保存 */
    void saveRunProgress(AgentRunProgress progress);
    /** @param result 期望终态 @return 数据库最终保存的执行结果，重复调用不能覆盖已有终态 @throws RuntimeException 持久化失败 */
    AgentRuntimeResult finishRun(AgentRuntimeResult result);
    /** 回收失联执行，由业务实现数据定位及事务，不允许自动重放模型调用。 */
    void finishExpiredRunList();
}
