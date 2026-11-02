package org.etd.framework.starter.ai.runtime.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Agent通用运行边界；不绑定供应商全局模型，预算按cl100k估算并需低于所选模型实际上下文限制。 */
@Data
@ConfigurationProperties("etd.ai.runtime")
public class AgentRuntimeProperties {
    /** 整轮最长执行秒数，包含检索和模型生成，默认180秒。 */
    private int timeoutSeconds = 180;
    /** 单节点并发轮数上限，默认32，超限立即拒绝。 */
    private int maxConcurrentRuns = 32;
    /** 输入估算Token预算，默认4096，不含输出预留，兼顾上下文深度与Prefill延迟。 */
    private int inputTokenBudget = 4096;
    /** 未配置Agent最大输出时使用的Token限制，默认2048。 */
    private int defaultMaxOutputTokens = 2048;
    /** 多库融合后实际纳入上下文的引用上限，默认4，保证权威证据并降低首字延迟。 */
    private int maxReferences = 4;
    /** Agent 未配置时的单 Run 图内模型调用上限，不含检索词改写，交由官方 ModelCallLimitHook 执行。 */
    private int modelCallLimit = 12;
    /** Agent 未配置时的单 Run 工具调用上限；达到后官方 Hook 会在下一次模型调用前终止。 */
    private int toolCallLimit = 24;
    /** 输出通道心跳间隔秒数，默认10秒。 */
    private int heartbeatSeconds = 10;
    /** 部分正文持久化间隔毫秒，默认2000毫秒。 */
    private int checkpointMillis = 2000;
    /** 助手正文字符硬上限，默认100000，防止异常服务无限输出。 */
    private int maxResponseCharacters = 100000;
}
