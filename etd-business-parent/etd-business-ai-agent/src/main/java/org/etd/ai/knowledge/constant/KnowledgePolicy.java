package org.etd.ai.knowledge.constant;

import java.util.Set;

/** 知识资料生命周期与本功能域协议约定。 */
public final class KnowledgePolicy {
    /** 资料管理与上传、处理入口共用的功能资源码。 */
    public static final String DOCUMENT_PERMISSION = "ai:knowledge-document";
    /** 允许单份资料生成的正文分片数量上限。 */
    public static final int MAX_CHUNKS = 10000;
    /** 单份正文分片的Token配置上限。 */
    public static final int MAX_CHUNK_TOKENS = 8192;
    /** 本地支持的检索策略。 */
    public static final String RETRIEVAL_PATTERN = "keyword|vector|hybrid";
    /** 原始文件摘要格式。 */
    public static final String SHA256_PATTERN = "^[0-9a-f]{64}$";
    /** 仅全文检索，无需生成向量。 */
    public static final String KEYWORD = "keyword";
    /** 向量检索。 */
    public static final String VECTOR = "vector";
    /** 混合检索。 */
    public static final String HYBRID = "hybrid";
    /** 分片会话初始化中。 */
    public static final String INITIALIZING = "initializing";
    /** 可以上传及续传。 */
    public static final String UPLOADING = "uploading";
    /** 合并及完整性核验中。 */
    public static final String COMPLETING = "completing";
    /** 完整文件已核验。 */
    public static final String COMPLETED = "completed";
    /** 会话已取消或过期。 */
    public static final String CANCELLED = "cancelled";
    /** 阶段执行失败。 */
    public static final String FAILED = "failed";
    /** 尚未启动解析。 */
    public static final String UPLOADED = "uploaded";
    /** 正文解析中。 */
    public static final String PARSING = "parsing";
    /** 等待用户确认分片。 */
    public static final String PREVIEW = "preview";
    /** 向量生成中。 */
    public static final String EMBEDDING = "embedding";
    /** 当前轮次可检索。 */
    public static final String READY = "ready";
    /** 待生成向量。 */
    public static final String PENDING = "pending";
    /** 全文模式无需向量。 */
    public static final String NOT_REQUIRED = "not_required";
    /** 当前存储实现。 */
    public static final String MINIO = "minio";
    /** S3最多允许10000个上传分片。 */
    public static final int MAX_PARTS = 10000;
    /** S3非末片最小为5MiB。 */
    public static final long MIN_PART_BYTES = 5L * 1024 * 1024;
    /** 当前数据库及向量索引支持的维度。 */
    public static final Set<Integer> DIMENSIONS = Set.of(768, 1024, 1536);
    private KnowledgePolicy() {}
}
