package org.etd.framework.starter.ai.runtime;

/** 本轮用量累计：单次模型调用取累计值的最大值，多次模型调用之间求和。 */
final class AgentTokenUsage {
    private long completedInputTokens;
    private long completedOutputTokens;
    private long currentInputTokens;
    private long currentOutputTokens;
    private Long totalInputTokens;
    private Long totalOutputTokens;

    /** 新调用开始时归集上一调用；未知用量保持为空，不伪造为零。 */
    synchronized void beginModelCall() {
        completedInputTokens += currentInputTokens;
        completedOutputTokens += currentOutputTokens;
        currentInputTokens = 0;
        currentOutputTokens = 0;
    }

    /** 分片可重复报告累计值，例如同一调用的 10、20、20 只计为 20。 */
    synchronized void recordCumulativeUsage(Long input, Long output) {
        if (input != null && input > currentInputTokens) {
            currentInputTokens = input;
            totalInputTokens = completedInputTokens + currentInputTokens;
        }
        if (output != null && output > currentOutputTokens) {
            currentOutputTokens = output;
            totalOutputTokens = completedOutputTokens + currentOutputTokens;
        }
    }

    /** 同一次加锁读取两个方向的用量，避免生成混合版本的快照。 */
    synchronized Snapshot snapshot() {
        return new Snapshot(totalInputTokens, totalOutputTokens);
    }

    /**
     * 已知用量快照；空值表示模型尚未报告对应方向的用量。
     * @param inputTokens 累计输入 Token 数
     * @param outputTokens 累计输出 Token 数
     */
    record Snapshot(Long inputTokens, Long outputTokens) {
    }
}
