package org.etd.ai.knowledge.constant;

/** 文档向量处理状态，与上传、分片确认及启停状态独立。 */
public final class KnowledgeVectorStatus {
    /** 尚未执行向量化，不表示已有后台任务。 */
    public static final String PENDING = "pending";
    /** 当前确认请求正在生成或写入向量。 */
    public static final String PROCESSING = "processing";
    /** 当前正文及知识库向量配置对应的向量已写入。 */
    public static final String READY = "ready";
    /** 最近一次向量生成或写入失败，可刷新版本后重新确认。 */
    public static final String FAILED = "failed";
    /** 正文、模型配置或在途确认版本变化，需要重新确认。 */
    public static final String STALE = "stale";
    /** 知识库采用全文检索，无需生成向量。 */
    public static final String NOT_REQUIRED = "not_required";

    private KnowledgeVectorStatus() { }

    /** @param status 变更前的向量状态 @return 原有或在途向量失效后的状态，尚未向量化及全文模式保持原语义 */
    public static String afterContentChange(String status) {
        if (PENDING.equals(status) || NOT_REQUIRED.equals(status)) {
            return status;
        }
        return STALE;
    }
}
