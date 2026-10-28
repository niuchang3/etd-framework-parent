package org.etd.framework.starter.ai.runtime.model;

/** 本轮终态，由业务生命周期插槽裁决后再通知接收端。
 * @param progress 最后正文及用量
 * @param status 实际或期望的终态
 * @param errorCode 稳定错误码
 * @param errorMessage 脱敏错误说明
 */
public record AgentRuntimeResult(AgentRunProgress progress, AgentRuntimeStatus status,
        String errorCode, String errorMessage) {
}
