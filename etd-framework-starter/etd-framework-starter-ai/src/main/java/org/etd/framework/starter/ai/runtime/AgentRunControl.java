package org.etd.framework.starter.ai.runtime;

/** Agent执行控制对象，用于查询执行标识和停止本节点任务；跨节点取消由运行记录服务处理。 */
public interface AgentRunControl {
    /** @return 当前执行标识 */
    String executionId();
    /** 取消上游调用，保留已生成内容；重复调用无副作用。 */
    void cancel();
}
