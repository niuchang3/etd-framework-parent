package org.etd.framework.starter.ai.persistence.spi;

import org.etd.framework.starter.ai.runtime.model.AgentRuntimeStatus;

/** 租约存储与失联回收；不混入聊天消息保存职责。 */
public interface AgentRunLeaseStore {
    /**
     * 框架定时在线程恢复用户/租户上下文后调用，原子续租并读取实际状态。
     * @param runId 当前 Run 标识
     * @return 数据库中的取消或运行状态；失败必须抛异常，不伪造续租成功
     */
    AgentRuntimeStatus renewRunLease(String runId);

    /**
     * 框架维护线程无用户身份地定时调用，必须限定为过期租约领取和终态修复。
     * 通过 Spring 代理开启短事务，条件更新保证重复调用和多节点竞争幂等。
     * 不得让扫描过程获得任意会话或工具访问资格；异常由框架记录，下一批重试。
     */
    void finishExpiredRunList();
}
