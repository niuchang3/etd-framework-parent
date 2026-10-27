package org.etd.ai.knowledge.constant;

/** 知识资料生命周期与本功能域协议约定。 */
public final class KnowledgePolicy {

    /** 文档与分片表供@DataPermissionTable声明的布尔共享列。 */
    public static final String PUBLIC_COLUMN = "is_public";

    /** 单次批量删除资料上限，避免长请求占用连接和对象存储资源。 */
    public static final int MAX_BATCH_DELETE = 100;

    /** 原始文件无扩展名时的展示类型，前端使用通用文件图标。 */
    public static final String UNKNOWN_FILE_TYPE = "unknown";

    /** 原始文件摘要格式。 */
    public static final String SHA256_PATTERN = "^[0-9a-f]{64}$";

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

    /** 当前存储实现。 */
    public static final String MINIO = "minio";
    /** S3最多允许10000个上传分片。 */
    public static final int MAX_PARTS = 10000;
    /** S3非末片最小为5MiB。 */
    public static final long MIN_PART_BYTES = 5L * 1024 * 1024;

    private KnowledgePolicy() {}
}
