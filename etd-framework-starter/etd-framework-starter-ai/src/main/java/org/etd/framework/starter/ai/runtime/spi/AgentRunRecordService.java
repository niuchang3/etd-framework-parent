package org.etd.framework.starter.ai.runtime.spi;

import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;

/** 业务运行记录接口；框架决定调用时机及运行流程，实现方只负责数据库状态与消息保存。 */
public interface AgentRunRecordService {
    /** 框架维护线程定期调用，已恢复本轮请求上下文；实现方只续租并读取权威状态，不创建定时任务。
     * 不得复活过期或已结束运行，允许与进度保存及结束回调并发，数据库条件更新应决定最终结果。
     * @param executionId 已授权执行标识 @return 续租后的状态，取消或失联时框架停止执行
     * @throws RuntimeException 读取或更新失败，框架停止本轮并尝试结束执行
     */
    AgentRuntimeStatus renewRunLease(String executionId);
    /** 框架按检查点间隔调用，实现方使用短事务保存，不能覆盖已经提交的终态。
     * @param progress 当前累计正文及实际展示引用 @throws RuntimeException 无法继续保存时令框架停止本轮
     */
    void saveRunProgress(AgentRunProgress progress);
    /** 正常结束、异常、取消及准备数据无效时调用，必须在短事务内原子保存运行终态和消息。
     * 已有取消请求与成功完成竞争时，由业务数据库规则裁决；重复调用必须返回已有结果，不覆盖终态。
     * @param result 框架期望终态 @return 数据库最终保存的非空执行结果，允许返回与期望不同的权威状态
     * @throws RuntimeException 持久化失败，不得假报已成功保存
     */
    AgentRuntimeResult finishRun(AgentRuntimeResult result);
    /** 框架后台定期调用，不带任何用户HTTP身份；实现方需明确维护任务的权限边界和短事务。
     * 只将符合租约到期条件的失联运行结束，不自动重放模型调用，不影响其他仍有效的运行。
     * @throws RuntimeException 回收失败，由框架记录诊断并在后续维护周期重试
     */
    void finishExpiredRunList();
}
