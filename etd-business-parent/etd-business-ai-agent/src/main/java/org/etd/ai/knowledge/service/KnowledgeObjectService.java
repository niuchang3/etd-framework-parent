package org.etd.ai.knowledge.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Part;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.model.UploadPart;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** 知识原件的MinIO适配，不读取系统参数中的文件扩展名筛选配置。 */
@Service
@RequiredArgsConstructor
public class KnowledgeObjectService {
    private final CustomMinioClient client;
    private final StorageProperties storage;
    private final KnowledgeUploadProperties properties;

    /** @return 后端配置的私有原件桶 */
    public String requireBucket() {
        String bucket = storage.getMinio().getDefaultBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new ApiRuntimeException("未配置知识资料存储桶");
        }
        return bucket;
    }

    /** @param document 由后端生成对象键的上传记录 @return MinIO上传会话ID */
    public String initUpload(KnowledgeDocumentEntity document) throws Exception {
        String bucket = document.getStorageBucket();
        var existsArgs = BucketExistsArgs.builder().bucket(bucket).build();
        if (!client.bucketExists(existsArgs).get()) {
            var createArgs = MakeBucketArgs.builder().bucket(bucket).build();
            client.makeBucket(createArgs).get();
        }
        return client.initMultipartUpload(bucket, null, document.getStorageObjectKey(), null, null);
    }

    /** @param document 会话记录 @param partNumber 分片编号 @return 临时PUT地址 */
    public String generatePartUrl(KnowledgeDocumentEntity document, int partNumber) throws Exception {
        Map<String, String> query = Map.of("uploadId", document.getUploadId(), "partNumber", String.valueOf(partNumber));
        long remaining = Duration.between(Instant.now(), document.getUploadExpiresAt()).getSeconds();
        if (remaining <= 0) {
            throw new ApiRuntimeException("上传会话已过期");
        }
        int expiry = (int) Math.min(properties.getUrlSeconds(), remaining);
        var args = GetPresignedObjectUrlArgs.builder().bucket(document.getStorageBucket())
                .object(document.getStorageObjectKey()).method(Method.PUT)
                .expiry(expiry).extraQueryParams(query).build();
        return client.getPresignedObjectUrl(args);
    }

    /** @param document 会话记录 @return 分页读取全部真实上传分片 */
    public List<UploadPart> fetchPartList(KnowledgeDocumentEntity document) throws Exception {
        var parts = new ArrayList<UploadPart>();
        int marker = 0;
        boolean truncated;
        do {
            var response = client.listPartsAsync(document.getStorageBucket(), null,
                    document.getStorageObjectKey(), 1000, marker, document.getUploadId(), null, null).get();
            var result = response.result();
            for (Part part : result.partList()) {
                var item = new UploadPart(part.partNumber(), part.etag(), part.partSize());
                parts.add(item);
            }
            truncated = result.isTruncated();
            marker = result.nextPartNumberMarker();
        } while (truncated);
        return parts;
    }

    /** @param document 持久化且经核实的分片清单；重复完成先查原件 */
    public void completeUpload(KnowledgeDocumentEntity document) throws Exception {
        if (existsObject(document)) {
            return;
        }
        var parts = new Part[document.getUploadParts().size()];
        for (int i = 0; i < parts.length; i++) {
            UploadPart item = document.getUploadParts().get(i);
            parts[i] = new Part(item.partNumber(), item.etag());
        }
        client.completeMultipart(document.getStorageBucket(), null, document.getStorageObjectKey(),
                document.getUploadId(), parts, null, null);
    }

    /** @param document 目标原件 @return 是否存在；仅NoSuchKey视为不存在 */
    public boolean existsObject(KnowledgeDocumentEntity document) throws Exception {
        var args = StatObjectArgs.builder().bucket(document.getStorageBucket()).object(document.getStorageObjectKey()).build();
        try {
            client.statObject(args).get();
            return true;
        } catch (ExecutionException exception) {
            if (hasErrorCode(exception, "NoSuchKey")) {
                return false;
            }
            throw exception;
        }
    }

    /** @param document 已合并原件；同时核验总大小及SHA-256 */
    public void verifyObject(KnowledgeDocumentEntity document) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        long size = 0;
        try (InputStream input = openObject(document)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
                size += count;
                if (size > document.getFileSize()) {
                    throw new ApiRuntimeException("合并文件大小不一致");
                }
                digest.update(buffer, 0, count);
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        if (size != document.getFileSize() || !actual.equals(document.getFileSha256())) {
            throw new ApiRuntimeException("合并文件大小或SHA-256不一致，请取消后重新上传");
        }
    }

    /** @param document 原件记录 @return 调用方负责关闭的读取流 */
    public InputStream openObject(KnowledgeDocumentEntity document) throws Exception {
        var args = GetObjectArgs.builder().bucket(document.getStorageBucket()).object(document.getStorageObjectKey()).build();
        return client.getObject(args).get();
    }

    /** @param document 已完成上传的资料 @return 临时下载地址 */
    public String generateDownloadUrl(KnowledgeDocumentEntity document) throws Exception {
        var args = GetPresignedObjectUrlArgs.builder().bucket(document.getStorageBucket())
                .object(document.getStorageObjectKey()).method(Method.GET).expiry(properties.getUrlSeconds()).build();
        return client.getPresignedObjectUrl(args);
    }

    /** @param document 上传会话；已终止的任务按成功处理 */
    public void abortUpload(KnowledgeDocumentEntity document) throws Exception {
        if (document.getUploadId() == null) {
            return;
        }
        try {
            client.abortMultipartUploadAsync(document.getStorageBucket(), null, document.getStorageObjectKey(),
                    document.getUploadId(), null, null).get();
        } catch (ExecutionException exception) {
            if (!hasErrorCode(exception, "NoSuchUpload")) {
                throw exception;
            }
        }
    }

    /** @param document 待清理的原件，删除不存在的对象也视为成功 */
    public void removeObject(KnowledgeDocumentEntity document) throws Exception {
        var args = RemoveObjectArgs.builder().bucket(document.getStorageBucket()).object(document.getStorageObjectKey()).build();
        client.removeObject(args).get();
    }
    /** SDK异步异常可能嵌套CompletionException，按根错误码识别正常的不存在状态。 */
    private boolean hasErrorCode(Throwable failure, String code) {
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause instanceof ErrorResponseException error && code.equals(error.errorResponse().code());
    }

}
