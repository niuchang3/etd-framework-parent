package org.etd.ai.agent.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 页面允许设置的通用生成参数，不允许任意服务商参数透传。 */
@Data
public class AgentModelOptions {
    /** 生成温度，通用校验范围为0至2；为空时不覆盖模型默认值，实际可用范围受模型限制。 */
    @DecimalMin("0")
    @DecimalMax("2")
    private Double temperature;

    /** 最大输出Token数，必须为正整数；为空时不覆盖模型默认值，实际限制受模型上下文约束。 */
    @Positive
    private Integer maxTokens;
}
