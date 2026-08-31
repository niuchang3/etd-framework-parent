package org.etd.framework.starter.storage.core.extend;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.model.*;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 阿里云 OSS 文件存储扩展实现
 *
 * @author Young
 */
@Slf4j
public class AlibabaOSSFileStorage extends FileStorage<OSSClient> {

    private final StorageProperties.AlibabaOSS properties;

    public AlibabaOSSFileStorage(StorageProperties.AlibabaOSS properties) {
        this.properties = properties;
    }

    // =========================================================================
    // 1. Web 前端直传能力
    // =========================================================================

    @Override
    public UploadUrlResModel generateUploadUrl(UploadUrlReqModel reqModel) throws Exception {
        try {
            String objectName = StringUtils.hasText(reqModel.getFileName())
                    ? buildObjectPath(reqModel.getDirectory(), reqModel.getFileName())
                    : buildObjectPath(reqModel.getDirectory(), generateFileName(reqModel.getOriginalFileName()));

            int expiry = reqModel.getExpiry() != null && reqModel.getExpiry() > 0
                    ? reqModel.getExpiry()
                    : properties.getExpiry();

            Date expiration = new Date(System.currentTimeMillis() + expiry * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(reqModel.getBucketName(), objectName, HttpMethod.PUT);
            request.setExpiration(expiration);
            if (StringUtils.hasText(reqModel.getContentType())) {
                request.setContentType(reqModel.getContentType());
            }

            URL url = getClient().generatePresignedUrl(request);

            UploadUrlResModel resModel = new UploadUrlResModel();
            resModel.setUploadUrl(url != null ? url.toString() : "");
            resModel.setFileName(objectName);
            resModel.setExpired(expiry);
            return resModel;
        } catch (Exception e) {
            log.error("Alibaba OSS generateUploadUrl failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public InitMultipartResModel initMultipart(InitMultipartReqModel reqModel) throws Exception {
        try {
            String objectName = StringUtils.hasText(reqModel.getFileName())
                    ? buildObjectPath(reqModel.getDirectory(), reqModel.getFileName())
                    : buildObjectPath(reqModel.getDirectory(), generateFileName(reqModel.getOriginalFileName()));

            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(reqModel.getBucketName(), objectName);
            if (StringUtils.hasText(reqModel.getContentType())) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(reqModel.getContentType());
                request.setObjectMetadata(metadata);
            }

            InitiateMultipartUploadResult initResult = getClient().initiateMultipartUpload(request);

            InitMultipartResModel resModel = new InitMultipartResModel();
            resModel.setUploadId(initResult.getUploadId());
            resModel.setFileName(objectName);
            return resModel;
        } catch (Exception e) {
            log.error("Alibaba OSS initMultipart failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public GeneratePartUrlResModel generatePartUrl(GeneratePartUrlReqModel reqModel) throws Exception {
        try {
            int expiry = reqModel.getExpiry() != null && reqModel.getExpiry() > 0
                    ? reqModel.getExpiry()
                    : properties.getExpiry();

            Date expiration = new Date(System.currentTimeMillis() + expiry * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(reqModel.getBucketName(), reqModel.getFileName(), HttpMethod.PUT);
            request.setExpiration(expiration);
            request.addQueryParameter("uploadId", reqModel.getUploadId());
            request.addQueryParameter("partNumber", String.valueOf(reqModel.getPartNumber()));

            URL url = getClient().generatePresignedUrl(request);

            GeneratePartUrlResModel resModel = new GeneratePartUrlResModel();
            resModel.setPartUrl(url != null ? url.toString() : "");
            resModel.setPartNumber(reqModel.getPartNumber());
            resModel.setExpired(expiry);
            return resModel;
        } catch (Exception e) {
            log.error("Alibaba OSS generatePartUrl failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public CompleteMultipartResModel completeMultipart(CompleteMultipartReqModel reqModel) throws Exception {
        try {
            List<PartETag> partETags = new ArrayList<>();
            if (reqModel.getParts() != null) {
                for (CompleteMultipartReqModel.PartETagInfo info : reqModel.getParts()) {
                    partETags.add(new PartETag(info.getPartNumber(), info.getETag()));
                }
            }

            CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                    reqModel.getBucketName(),
                    reqModel.getFileName(),
                    reqModel.getUploadId(),
                    partETags
            );

            CompleteMultipartUploadResult result = getClient().completeMultipartUpload(request);

            CompleteMultipartResModel resModel = new CompleteMultipartResModel();
            resModel.setFileName(reqModel.getFileName());
            resModel.setFileUrl(result != null ? result.getLocation() : "");
            return resModel;
        } catch (Exception e) {
            log.error("Alibaba OSS completeMultipart failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // =========================================================================
    // 2. 服务端直接上传能力
    // =========================================================================

    @Override
    public ServerUploadResModel upload(InputStreamUploadReqModel reqModel) throws Exception {
        try {
            String objectName = StringUtils.hasText(reqModel.getFileName())
                    ? buildObjectPath(reqModel.getDirectory(), reqModel.getFileName())
                    : buildObjectPath(reqModel.getDirectory(), generateFileName(reqModel.getOriginalFileName()));

            ObjectMetadata metadata = new ObjectMetadata();
            if (StringUtils.hasText(reqModel.getContentType())) {
                metadata.setContentType(reqModel.getContentType());
            }
            if (reqModel.getFileSize() > 0) {
                metadata.setContentLength(reqModel.getFileSize());
            }

            getClient().putObject(reqModel.getBucketName(), objectName, reqModel.getInputStream(), metadata);

            Date expiration = new Date(System.currentTimeMillis() + properties.getExpiry() * 1000L);
            URL url = getClient().generatePresignedUrl(reqModel.getBucketName(), objectName, expiration);

            ServerUploadResModel resModel = new ServerUploadResModel();
            resModel.setFileName(objectName);
            resModel.setFileUrl(url != null ? url.toString() : "");
            return resModel;
        } catch (Exception e) {
            log.error("Alibaba OSS server upload InputStream failed: {}", e.getMessage(), e);
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
            log.error("Alibaba OSS server upload byte[] failed: {}", e.getMessage(), e);
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
            log.error("Alibaba OSS server upload MultipartFile failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
