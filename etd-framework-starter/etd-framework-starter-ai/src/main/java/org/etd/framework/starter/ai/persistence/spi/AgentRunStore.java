package org.etd.framework.starter.ai.persistence.spi;

import org.etd.framework.starter.ai.runtime.model.AgentRunProgress;
import org.etd.framework.starter.ai.runtime.model.AgentRuntimeResult;

/**
 * 消息与运行结果持久化契约：由业务模块提供短事务实现。
 * 框架仅在部分正文 checkpoint 和终态落库时调用；租约协调由独立接口承担。
 */
public interface AgentRunStore {

    /**
     * 框架维护线程在可信上下文内保存部分正文快照，可能与终态竞争。
     * 实现应使用独立短事务与条件更新；不能覆盖终态，不能以失败快照清空已保存正文。
     * @param progress 本轮累积正文、实际引用与用量
     * @throws RuntimeException 失去租约或保存失败，框架终止生成
     */
    void saveRunProgress(AgentRunProgress progress);

    /**
     * 框架工作线程退出生成后调用；短事务原子保存消息和 Run 结果。
     * 多节点取消和重复终结由数据库条件更新裁决，必须幂等；失败抛异常，禁止先发终态事件。
     * @param result 框架期望的终态
     * @return 经数据库持久化确认后的实际结果，取消竞争优先沿用已确认规则
     * @throws RuntimeException 存储失败，由受限的租约回收流程修复
     */
    AgentRuntimeResult finishRun(AgentRuntimeResult result);

}
