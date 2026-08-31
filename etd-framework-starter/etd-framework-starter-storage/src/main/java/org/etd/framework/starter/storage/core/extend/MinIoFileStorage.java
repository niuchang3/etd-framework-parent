package org.etd.framework.starter.storage.core.extend;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.model.*;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * MinIO 文件存储扩展实现
 *
 * @author Young
 */
@Slf4j
public class MinIoFileStorage extends FileStorage<CustomMinioClient> {

    private final StorageProperties.MinIo properties;

    public MinIoFileStorage(StorageProperties.MinIo properties) {
        this.properties = properties;
    }

    @Override
    protected void ensureBucketExists(String bucketName) {
        try {
            boolean exists = getClient().bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()).get();
            if (!exists) {
                log.info("MinIO bucket [{}] does not exist, creating automatically...", bucketName);
                getClient().makeBucket(MakeBucketArgs.builder().bucket(bucketName).build()).get();

                String policyConfig = properties.getPolicy();
                if (StringUtils.hasText(policyConfig)) {
                    if ("public-read".equalsIgnoreCase(policyConfig) || "public".equalsIgnoreCase(policyConfig)) {
                        String readOnlyPolicy = """
                                {
                                  "Version": "2012-10-17",
                                  "Statement": [
                                    {
                                      "Effect": "Allow",
                                      "Principal": "*",
                                      "Action": ["s3:GetObject"],
                                      "Resource": ["arn:aws:s3:::%s/*"]
                                    }
                                  ]
                                }
                                """.formatted(bucketName);
                        getClient().setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(readOnlyPolicy).build()).get();
                        log.info("MinIO bucket [{}] set policy to 'public-read' successfully", bucketName);
                    } else if ("public-read-write".equalsIgnoreCase(policyConfig)) {
                        String readWritePolicy = """
                                {
                                  "Version": "2012-10-17",
                                  "Statement": [
                                    {
                                      "Effect": "Allow",
                                      "Principal": "*",
                                      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"],
                                      "Resource": ["arn:aws:s3:::%s/*"]
                                    }
                                  ]
                                }
                                """.formatted(bucketName);
                        getClient().setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(readWritePolicy).build()).get();
                        log.warn("MinIO bucket [{}] set policy to 'public-read-write' (HIGH RISK!)", bucketName);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("MinIO check/create bucket [{}] encountered warning: {}", bucketName, e.getMessage());
        }
    }

    // =========================================================================
    // 1. Web 前端直传能力
    // =========================================================================

    @Override
    public UploadUrlResModel generateUploadUrl(UploadUrlReqModel reqModel) throws Exception {
        try {
            String bucketName = getRealBucketName(reqModel.getBucketName(), properties.getDefaultBucket());
            ensureBucketExists(bucketName);

            String objectName = StringUtils.hasText(reqModel.getFileName())
                    ? buildObjectPath(reqModel.getDirectory(), reqModel.getFileName())
                    : buildObjectPath(reqModel.getDirectory(), generateFileName(reqModel.getOriginalFileName()));

            int expiry = reqModel.getExpiry() != null && reqModel.getExpiry() > 0
                    ? reqModel.getExpiry()
                    : properties.getExpiry();

            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expiry);

            String uploadUrl = getClient().getPresignedObjectUrl(builder.build());

            UploadUrlResModel resModel = new UploadUrlResModel();
            resModel.setUploadUrl(uploadUrl);
            resModel.setFileName(objectName);
            resModel.setExpired(expiry);
            return resModel;
        } catch (Exception e) {
            log.error("MinIO generateUploadUrl failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public InitMultipartResModel initMultipart(InitMultipartReqModel reqModel) throws Exception {
        try {
            String bucketName = getRealBucketName(reqModel.getBucketName(), properties.getDefaultBucket());
            ensureBucketExists(bucketName);

            String objectName = StringUtils.hasText(reqModel.getFileName())
                    ? buildObjectPath(reqModel.getDirectory(), reqModel.getFileName())
                    : buildObjectPath(reqModel.getDirectory(), generateFileName(reqModel.getOriginalFileName()));

            String uploadId = getClient().initMultipartUpload(
                    bucketName,
                    null,
                    objectName,
                    null,
                    null
            );

            InitMultipartResModel resModel = new InitMultipartResModel();
            resModel.setUploadId(uploadId);
            resModel.setFileName(objectName);
            return resModel;
        } catch (Exception e) {
            log.error("MinIO initMultipart failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public GeneratePartUrlResModel generatePartUrl(GeneratePartUrlReqModel reqModel) throws Exception {
        try {
            String bucketName = getRealBucketName(reqModel.getBucketName(), properties.getDefaultBucket());
            int expiry = reqModel.getExpiry() != null && reqModel.getExpiry() > 0
                    ? reqModel.getExpiry()
                    : properties.getExpiry();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("uploadId", reqModel.getUploadId());
            queryParams.put("partNumber", String.valueOf(reqModel.getPartNumber()));

            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucketName)
                    .object(reqModel.getFileName())
                    .expiry(expiry)
                    .extraQueryParams(queryParams);

            String partUrl = getClient().getPresignedObjectUrl(builder.build());

            GeneratePartUrlResModel resModel = new GeneratePartUrlResModel();
            resModel.setPartUrl(partUrl);
            resModel.setPartNumber(reqModel.getPartNumber());
            resModel.setExpired(expiry);
            return resModel;
        } catch (Exception e) {
            log.error("MinIO generatePartUrl failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public CompleteMultipartResModel completeMultipart(CompleteMultipartReqModel reqModel) throws Exception {
        try {
            String bucketName = getRealBucketName(reqModel.getBucketName(), properties.getDefaultBucket());
            Part[] minioParts = new Part[reqModel.getParts() != null ? reqModel.getParts().size() : 0];
            if (reqModel.getParts() != null) {
                for (int i = 0; i < reqModel.getParts().size(); i++) {
                    CompleteMultipartReqModel.PartETagInfo info = reqModel.getParts().get(i);
                    minioParts[i] = new Part(info.getPartNumber(), info.getETag());
                }
            }

            getClient().completeMultipart(
                    bucketName,
                    null,
                    reqModel.getFileName(),
                    reqModel.getUploadId(),
                    minioParts,
                    null,
                    null
            );

            String fileUrl = getClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(reqModel.getFileName())
                            .expiry(properties.getExpiry())
                            .build()
            );

            CompleteMultipartResModel resModel = new CompleteMultipartResModel();
            resModel.setFileName(reqModel.getFileName());
            resModel.setFileUrl(fileUrl);
            return resModel;
        } catch (Exception e) {
            log.error("MinIO completeMultipart failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // =========================================================================
    // 2. 服务端直接上传能力
    // =========================================================================

    @Override
    public ServerUploadResModel upload(InputStreamUploadReqModel reqModel) throws Exception {
        try {
            String bucketName = getRealBucketName(reqModel.getBucketName(), properties.getDefaultBucket());
            ensureBucketExists(bucketName);

            String objectName = StringUtils.hasText(reqModel.getFileName())
                    ? buildObjectPath(reqModel.getDirectory(), reqModel.getFileName())
                    : buildObjectPath(reqModel.getDirectory(), generateFileName(reqModel.getOriginalFileName()));

            long objectSize = reqModel.getFileSize() > 0 ? reqModel.getFileSize() : -1;
            long partSize = objectSize > 0 ? -1 : 10 * 1024 * 1024;

            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(reqModel.getInputStream(), objectSize, partSize);

            if (StringUtils.hasText(reqModel.getContentType())) {
                builder.contentType(reqModel.getContentType());
            }

            getClient().putObject(builder.build());

            String fileUrl = getClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(properties.getExpiry())
                            .build()
            );

            ServerUploadResModel resModel = new ServerUploadResModel();
            resModel.setFileName(objectName);
            resModel.setFileUrl(fileUrl);
            return resModel;
        } catch (Exception e) {
            log.error("MinIO server upload InputStream failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ServerUploadResModel upload(ByteUploadReqModel reqModel) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(reqModel.getBytes())) {
            InputStreamUploadReqModel uploadReq = new InputStreamUploadReqModel();
            uploadReq.setBucketName(reqModel.getBucketName());
            uploadReq.setDirectory(reqModel.getDirectory());
            uploadReq.setOriginalFileName(reqModel.getOriginalFileName());
            uploadReq.setFileName(reqModel.getFileName());
            uploadReq.setContentType(reqModel.getContentType());
            uploadReq.setInputStream(inputStream);
            uploadReq.setFileSize(reqModel.getBytes() != null ? reqModel.getBytes().length : 0);
            return upload(uploadReq);
        } catch (Exception e) {
            log.error("MinIO server upload byte[] failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ServerUploadResModel upload(MultipartUploadReqModel reqModel) throws Exception {
        try (InputStream inputStream = reqModel.getFile().getInputStream()) {
            InputStreamUploadReqModel uploadReq = new InputStreamUploadReqModel();
            uploadReq.setBucketName(reqModel.getBucketName());
            uploadReq.setDirectory(reqModel.getDirectory());
            uploadReq.setOriginalFileName(reqModel.getFile().getOriginalFilename());
            uploadReq.setContentType(reqModel.getFile().getContentType());
            uploadReq.setInputStream(inputStream);
            uploadReq.setFileSize(reqModel.getFile().getSize());
            return upload(uploadReq);
        } catch (Exception e) {
            log.error("MinIO server upload MultipartFile failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // =========================================================================
    // 3. 在线预览/下载链接生成与物理流式下载能力
    // =========================================================================

    @Override
    public GenerateObjectUrlResModel generateObjectUrl(GenerateObjectUrlReqModel reqModel) throws Exception {
        try {
            String bucketName = getRealBucketName(reqModel.getBucketName(), properties.getDefaultBucket());
            int expiry = reqModel.getExpiry() != null && reqModel.getExpiry() > 0
                    ? reqModel.getExpiry()
                    : properties.getExpiry();

            String fileUrl = getClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(reqModel.getFileName())
                            .expiry(expiry)
                            .build()
            );

            GenerateObjectUrlResModel resModel = new GenerateObjectUrlResModel();
            resModel.setFileUrl(fileUrl);
            resModel.setExpired(expiry);
            return resModel;
        } catch (Exception e) {
            log.error("MinIO generateObjectUrl failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public InputStream downloadInputStream(DownloadObjectReqModel reqModel) throws Exception {
        try {
            String bucketName = getRealBucketName(reqModel.getBucketName(), properties.getDefaultBucket());
            GetObjectResponse response = getClient().getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(reqModel.getFileName())
                            .build()
            ).get();
            return response;
        } catch (Exception e) {
            log.error("MinIO downloadInputStream failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public byte[] downloadBytes(DownloadObjectReqModel reqModel) throws Exception {
        try (InputStream inputStream = downloadInputStream(reqModel)) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("MinIO downloadBytes failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
