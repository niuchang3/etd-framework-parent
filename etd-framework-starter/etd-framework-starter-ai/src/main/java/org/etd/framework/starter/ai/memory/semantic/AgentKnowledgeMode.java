package org.etd.framework.starter.ai.memory.semantic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 显式选择知识接入方式；同一 Run 不同时安装预检索与检索工具。 */
public enum AgentKnowledgeMode {
    /** 不安装知识能力。 */
    NONE("none"),
    /** 保留现有回答前预检索行为。 */
    PRELOAD("preload"),
    /** 官方模型决策何时调用知识工具。 */
    TOOL("tool");

    private final String code;

    AgentKnowledgeMode(String code) {
        this.code = code;
    }

    /** @return 稳定配置值，不依赖枚举名称或序号 */
    @JsonValue
    public String getCode() {
        return code;
    }

    /** @param code 明确的配置值 @return 对应模式 @throws IllegalArgumentException 非法模式 */
    @JsonCreator
    public static AgentKnowledgeMode fromCode(String code) {
        for (AgentKnowledgeMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("不支持的知识接入模式");
    }
}
