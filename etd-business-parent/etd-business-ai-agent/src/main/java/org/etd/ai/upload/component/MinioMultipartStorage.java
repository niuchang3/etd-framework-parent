package org.etd.ai.upload.component;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Part;

import lombok.RequiredArgsConstructor;

import org.etd.ai.upload.entity.UploadSessionEntity;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.storage.core.FileUpload;
import org.etd.framework.starter.storage.model.*;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** 复用存储starter的初始化、分片签名和合并，SDK补充校验及清理，不暴露存储凭证。 */
@Component
@RequiredArgsConstructor
public class MinioMultipartStorage {
    private final ObjectProvider<CustomMinioClient> clients;
    private final ObjectProvider<FileUpload> uploads;
    private final StorageProperties properties;

    /** 对象存储异步请求的最大等待秒数。 */
    private static final int IO_TIMEOUT_SECONDS = 30;

    /**
     * 本模块仅允许私有桶，公开资料仍需SaaS认证和授权。
     *
     * @return 配置指定的私有MinIO桶名
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public String requireBucket() {
        if (!"private".equals(properties.getMinio().getPolicy())
                || !properties.getMinio().isEnabled()
                || properties.getMinio().getDefaultBucket() == null
                || properties.getMinio().getDefaultBucket().isBlank()) {
            throw new ApiRuntimeException("AI文件需要启用私有MinIO存储");
        }
        return properties.getMinio().getDefaultBucket();
    }

    /**
     * 初始化真实任务，显式使用后端唯一对象键。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @return MinIO创建的真实分片任务ID，仅保存于后端
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public String initialize(UploadSessionEntity s) throws Exception {
        InitMultipartReqModel req = new InitMultipartReqModel();
        req.setBucketName(s.getBucket());
        req.setFileName(s.getObjectKey());
        req.setOriginalFileName(s.getOriginalFilename());
        return uploads.getObject().initMultipart(req).getUploadId();
    }

    /**
     * 按后端任务快照签发单片PUT URL。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @param number 从1开始的分片序号，不能超过任务总片数
     * @param seconds 签名有效秒数，不能超过任务剩余有效期
     * @return 指定分片的短期PUT签名地址
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public String signPart(UploadSessionEntity s, int number, int seconds) throws Exception {
        GeneratePartUrlReqModel req = new GeneratePartUrlReqModel();
        req.setBucketName(s.getBucket());
        req.setFileName(s.getObjectKey());
        req.setUploadId(s.getUploadId());
        req.setPartNumber(number);
        req.setExpiry(seconds);
        return uploads.getObject().generatePartUrl(req).getPartUrl();
    }

    /**
     * 从MinIO取得实际分片清单，处理1000片以上分页。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @return 符合条件的记录集合，不在Java中拼接跨表权限数据
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public List<Part> fetchPartList(UploadSessionEntity s) throws Exception {
        List<Part> parts = new ArrayList<>();
        int marker = 0;
        while (true) {
            var result =
                    clients.getObject()
                            .listPartsAsync(
                                    s.getBucket(),
                                    null,
                                    s.getObjectKey(),
                                    1000,
                                    marker,
                                    s.getUploadId(),
                                    null,
                                    null)
                            .get(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .result();
            parts.addAll(result.partList());
            if (!result.isTruncated()) {
                return parts;
            }
            if (result.nextPartNumberMarker() <= marker) {
                throw new ApiRuntimeException("存储分片分页异常");
            }
            marker = result.nextPartNumberMarker();
        }
    }

    /**
     * 已合并对象可用于恢复响应丢失后的幂等完成；仅对象不存在时返回false。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @return 目标对象是否存在
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public boolean exists(UploadSessionEntity s) throws Exception {
        try {
            var request =
                    StatObjectArgs.builder().bucket(s.getBucket()).object(s.getObjectKey()).build();
            clients.getObject().statObject(request).get(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (ExecutionException e) {
            if (isStorageError(e, "NoSuchKey")) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 仅使用已与存储实际清单核对的分片完成合并。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @param parts 按序提交的完整分片清单，合并前核对存储实际数据
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public void complete(UploadSessionEntity s, List<CompleteMultipartReqModel.PartETagInfo> parts)
            throws Exception {
        CompleteMultipartReqModel req = new CompleteMultipartReqModel();
        req.setBucketName(s.getBucket());
        req.setFileName(s.getObjectKey());
        req.setUploadId(s.getUploadId());
        req.setParts(parts);
        uploads.getObject().completeMultipart(req);
    }

    /**
     * 读取私有对象流，调用者必须关闭并校验实际字节数。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @return 通过本方法校验或生成的业务结果
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public InputStream open(UploadSessionEntity s) throws Exception {
        var request =
                GetObjectArgs.builder().bucket(s.getBucket()).object(s.getObjectKey()).build();
        return clients.getObject().getObject(request).get(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 经文件权限校验后签发短期下载链接。
     *
     * @param filename 核验后的原始文件名，仅用于下载响应头
     * @param bucket 后端确定的私有存储桶
     * @param key 后端生成并限定租户边界的对象键
     * @param seconds 签名有效秒数，不能超过任务剩余有效期
     * @return 原件的短期GET签名地址，带安全下载文件名
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public String signDownload(String bucket, String key, int seconds, String filename)
            throws Exception {
        String encodedFilename =
                URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        var responseParameters =
                Map.of(
                        "response-content-disposition",
                        "attachment; filename*=UTF-8''" + encodedFilename);
        var request =
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(key)
                        .expiry(seconds)
                        .extraQueryParams(responseParameters)
                        .build();
        return clients.getObject().getPresignedObjectUrl(request);
    }

    /**
     * 幂等删除不再被业务文件引用的原件。
     *
     * @param bucket 后端确定的私有存储桶
     * @param key 后端生成并限定租户边界的对象键
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public void remove(String bucket, String key) throws Exception {
        var request = RemoveObjectArgs.builder().bucket(bucket).object(key).build();
        clients.getObject().removeObject(request).get(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 中止已知任务；初始化登记中断时按后端唯一对象键查找，绝不中止其他文件。
     *
     * @param bucket 后端确定的私有存储桶
     * @param key 后端生成并限定租户边界的对象键
     * @param uploadId 后端取得的MinIO任务标识，不接受客户端任意指定
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public void abort(String bucket, String key, String uploadId) throws Exception {
        if (uploadId != null) {
            abortKnown(bucket, key, uploadId);
            return;
        }
        var result =
                clients.getObject()
                        .listMultipartUploadsAsync(
                                bucket, null, null, null, null, 1000, key, null, null, null)
                        .get(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .result();
        for (var upload : result.uploads()) {
            if (key.equals(upload.objectName())) {
                abortKnown(bucket, key, upload.uploadId());
            }
        }
    }

    /** SDK异步调用可能多层包装CompletionException，只将明确的不存在响应视为幂等结果。 */
    private boolean isStorageError(Throwable exception, String code) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ErrorResponseException error) {
                return code.equals(error.errorResponse().code());
            }
            current = current.getCause();
        }
        return false;
    }

    private void abortKnown(String bucket, String key, String uploadId) throws Exception {
        try {
            clients.getObject()
                    .abortMultipartUploadAsync(bucket, null, key, uploadId, null, null)
                    .get(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (!isStorageError(e, "NoSuchUpload")) {
                throw e;
            }
        }
    }
}
