package io.minio;

import com.google.common.collect.Multimap;
import io.minio.messages.Part;

/**
 * MinIO 异步扩展客户端，位于 io.minio 包下以无缝调用原生 S3 分片任务 API
 *
 * @author Young
 */
public class CustomMinioClient extends MinioAsyncClient {

    public CustomMinioClient(MinioAsyncClient client) {
        super(client);
    }

    /**
     * 初始化 S3 分片任务
     */
    public String initMultipartUpload(String bucketName, String region, String objectName, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws Exception {
        return this.createMultipartUploadAsync(bucketName, region, objectName, headers, extraQueryParams).get().result().uploadId();
    }

    /**
     * 完成 S3 分片任务合并
     */
    public ObjectWriteResponse completeMultipart(String bucketName, String region, String objectName, String uploadId, Part[] parts, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws Exception {
        return this.completeMultipartUploadAsync(bucketName, region, objectName, uploadId, parts, headers, extraQueryParams).get();
    }
}
