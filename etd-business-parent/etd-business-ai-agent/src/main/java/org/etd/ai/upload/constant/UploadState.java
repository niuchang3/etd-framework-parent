package org.etd.ai.upload.constant;

/** 文件传输状态，与文档解析工作流分开。 */
public final class UploadState {
    /** 已持久化任务，存储初始化尚未登记。 */
    public static final String INITIALIZING = "initializing";

    /** 可申请分片签名。 */
    public static final String UPLOADING = "uploading";

    /** 已登记文件，完成确认幂等。 */
    public static final String COMPLETED = "completed";

    /** 已取消或已过期，等待异步清理。 */
    public static final String CANCELLED = "cancelled";

    private UploadState() {}
}
