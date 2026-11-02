package org.etd.framework.starter.ai.memory.semantic;

/** 本轮查询向量的空间身份；相同维度但连接不同也不能复用。
 * @param connectionId 已授权的模型连接标识
 * @param dimensions 向量维度，例如 768、1024 或 1536
 */
public record EmbeddingSpace(Long connectionId, Integer dimensions) {
}
