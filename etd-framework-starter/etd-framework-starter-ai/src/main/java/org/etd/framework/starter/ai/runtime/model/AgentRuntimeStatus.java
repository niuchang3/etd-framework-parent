package org.etd.framework.starter.ai.runtime.model;

/** 运行状态协议；业务存储可映射自己的状态，但不能将取消请求提前视为成功。 */
public enum AgentRuntimeStatus {
    /** 正常执行。 */
    RUNNING("running"),
    /** 成功结束。 */
    SUCCEEDED("succeeded"),
    /** 执行失败。 */
    FAILED("failed"),
    /** 已取消。 */
    CANCELLED("cancelled");
    private final String code;
    AgentRuntimeStatus(String code) {
        this.code = code;
    }
    /** @return 明确的状态协议值，不使用ordinal或枚举名称 */
    public String getCode() {
        return code;
    }
}
