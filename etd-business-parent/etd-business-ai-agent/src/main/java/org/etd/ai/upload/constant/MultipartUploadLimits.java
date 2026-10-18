package org.etd.ai.upload.constant;

/** MinIO兼容S3的传输协议边界，不包含客户端维护的文件类型及大小业务限制。 */
public final class MultipartUploadLimits {
    /** 除最后一片外，普通分片至少为5MiB。 */
    public static final long MIN_PART_BYTES = 5L * 1024 * 1024;

    /** 单个分片最多为5GiB。 */
    public static final long MAX_PART_BYTES = 5L * 1024 * 1024 * 1024;

    /** 一次分片上传最多允许10000片。 */
    public static final int MAX_PART_COUNT = 10000;

    /** S3预签名URL最长有效7天，单位为秒。 */
    public static final int MAX_URL_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    private MultipartUploadLimits() {}
}
