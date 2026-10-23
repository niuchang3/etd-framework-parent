package org.etd.ai.knowledge.constant;

/** 分片草稿生命周期，与文件上传状态及向量处理无关。 */
public final class KnowledgeChunkStatus {
    /** 尚未生成分片。 */
    public static final String UNPROCESSED = "unprocessed";
    /** 正在生成新的分片结果，现有结果暂时只读。 */
    public static final String GENERATING = "generating";
    /** 分片可编辑，尚未确认。 */
    public static final String DRAFT = "draft";
    /** 用户已确认当前结果。 */
    public static final String CONFIRMED = "confirmed";
    /** 本次生成失败，已有结果仍保留。 */
    public static final String FAILED = "failed";
    private KnowledgeChunkStatus() {}
}
