package org.etd.framework.starter.ai.runtime.model;

import java.util.Map;
import org.etd.framework.starter.ai.adapter.AgentRunEventListener;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;

/** 框架内部任务输入：纯授权数据、记录与租约存储、输出适配；不含模型或检索回调。
 * @param preparation 业务短事务返回的数据
 * @param question 本轮问题
 * @param runStore 正文与结果存储
 * @param leaseStore 租约协调存储
 * @param eventListener 每次请求独立的事件通道
 */
public record AgentRunRequest(AgentChatPreparation preparation, String question, AgentRunStore runStore,
        AgentRunLeaseStore leaseStore, AgentRunEventListener eventListener) {
    /** @return 唯一 Run 标识，任务不创建第二个 Execution 实体 */
    public String runId() {
        return preparation.runId();
    }

    /** @return 已授权的协议关联数据，不包含可信身份 */
    public Map<String, Object> attributes() {
        return preparation.attributes();
    }
}
