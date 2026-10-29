package org.etd.framework.starter.ai.runtime.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Agent通用运行边界；不绑定供应商全局模型，预算按cl100k估算并需低于所选模型实际上下文限制。 */
@Data
@Validated
@ConfigurationProperties("etd.ai.runtime")
public class AgentRuntimeProperties {
    /** 整轮最长执行秒数，包含检索和模型生成，默认180秒。 */
    @Min(10) @Max(3600)
    private int timeoutSeconds = 180;
    /** 单节点并发轮数上限，默认32，超限立即拒绝。 */
    @Min(1) @Max(1000)
    private int maxConcurrentRuns = 32;
    /** 输入估算Token预算，默认4096，不含输出预留，兼顾上下文深度与Prefill延迟。 */
    @Min(256) @Max(200000)
    private int inputTokenBudget = 4096;
    /** 未配置Agent最大输出时使用的Token限制，默认2048。 */
    @Min(1) @Max(32768)
    private int defaultMaxOutputTokens = 2048;
    /** 多库融合后实际纳入上下文的引用上限，默认4，保证权威证据并降低首字延迟。 */
    @Min(1) @Max(100)
    private int maxReferences = 4;
    /** 输出通道心跳间隔秒数，默认10秒。 */
    @Min(1) @Max(60)
    private int heartbeatSeconds = 10;
    /** 部分正文持久化间隔毫秒，默认2000毫秒。 */
    @Min(500) @Max(10000)
    private int checkpointMillis = 2000;
    /** 助手正文字符硬上限，默认100000，防止异常服务无限输出。 */
    @Min(1000) @Max(1000000)
    private int maxResponseCharacters = 100000;
}
