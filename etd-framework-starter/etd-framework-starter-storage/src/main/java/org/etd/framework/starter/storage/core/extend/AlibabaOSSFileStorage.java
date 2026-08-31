package org.etd.framework.starter.storage.core.extend;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.model.UploadResModel;
import org.etd.framework.starter.storage.model.extend.ByteUploadModel;
import org.etd.framework.starter.storage.model.extend.InputStreamUploadModel;
import org.etd.framework.starter.storage.model.extend.MultipartFileUploadModel;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.util.ObjectUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Override
    public UploadResModel upload(MultipartFileUploadModel uploadModel) throws Exception {
        InputStreamUploadModel upload = new InputStreamUploadModel();
        upload.setInputStream(uploadModel.getFile().getInputStream());
        upload.setFileName(uploadModel.getFile().getOriginalFilename());
        upload.setContentType(uploadModel.getFile().getContentType());
        upload.setFileSize(uploadModel.getFile().getSize());
        upload.setBucketName(uploadModel.getBucketName());
        upload.setDirectory(uploadModel.getDirectory());
        return upload(upload);
    }

    @Override
    public UploadResModel upload(InputStreamUploadModel uploadModel) throws Exception {
        autoMakeBucket(uploadModel.getBucketName());
        try (InputStream inputStream = uploadModel.getInputStream()) {
            String fileName = buildObjectPath(uploadModel.getDirectory(), generateFileName(uploadModel.getFileName()));
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(uploadModel.getContentType());
            if (uploadModel.getFileSize() > 0) {
                metadata.setContentLength(uploadModel.getFileSize());
            }
            PutObjectRequest request = new PutObjectRequest(uploadModel.getBucketName(), fileName, inputStream, metadata);
            getClient().putObject(request);

            UploadResModel resModel = new UploadResModel();
            resModel.setFileName(fileName);
            resModel.setFileUrl(getObjectUrl(uploadModel.getBucketName(), fileName));
            resModel.setExpired(properties.getExpiry());
            return resModel;
        } catch (Exception e) {
            log.error("Alibaba OSS upload file failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public UploadResModel upload(ByteUploadModel uploadModel) throws Exception {
        autoMakeBucket(uploadModel.getBucketName());

        String fileName = buildObjectPath(uploadModel.getDirectory(), generateFileName(uploadModel.getFileName()));
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(uploadModel.getFile());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(uploadModel.getContentType());
        metadata.setContentLength(uploadModel.getFile().length);
        PutObjectRequest request = new PutObjectRequest(uploadModel.getBucketName(), fileName, byteArrayInputStream, metadata);
        getClient().putObject(request);

        UploadResModel resModel = new UploadResModel();
        resModel.setFileName(fileName);
        resModel.setFileUrl(getObjectUrl(uploadModel.getBucketName(), fileName));
        resModel.setExpired(properties.getExpiry());
        return resModel;
    }

    @Override
    public void remove(String bucketName, String fileName) throws Exception {
        try {
            getClient().deleteObject(bucketName, fileName);
        } catch (Exception e) {
            log.error("Alibaba OSS remove file failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void remove(String bucketName, List<String> fileNames) throws Exception {
        try {
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
            request.setKeys(fileNames);
            getClient().deleteObjects(request);
        } catch (Exception e) {
            log.error("Alibaba OSS batch remove files failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean bucketExists(String bucketName) {
        try {
            return getClient().doesBucketExist(bucketName);
        } catch (Exception e) {
            log.error("Alibaba OSS check bucketExists failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void makeBucket(String bucketName) {
        try {
            getClient().createBucket(bucketName);
        } catch (Exception e) {
            log.error("Alibaba OSS makeBucket failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void autoMakeBucket(String bucketName) {
        if (!Boolean.TRUE.equals(properties.getAutoCreateBucket())) {
            return;
        }
        if (bucketExists(bucketName)) {
            return;
        }
        makeBucket(bucketName);
    }

    @Override
    public String getObjectUrl(String bucketName, String fileName) throws Exception {
        long dateTime = System.currentTimeMillis() + (properties.getExpiry() * 1000L);
        URL url = getClient().generatePresignedUrl(bucketName, fileName, new Date(dateTime));
        return ObjectUtils.isEmpty(url) ? "" : url.toString();
    }

    @Override
    public void download(HttpServletResponse response, String bucketName, String fileName) throws Exception {
        download(response, bucketName, fileName, null);
    }

    @Override
    public void download(HttpServletResponse response, String bucketName, String fileName, String originalFileName) throws Exception {
        String downLoadFileName = ObjectUtils.isEmpty(originalFileName) ? fileName : originalFileName;
        response.setHeader(HeaderConstant.CONTENT_DISPOSITION, "attachment;filename=" + URLEncoder.encode(downLoadFileName, StandardCharsets.UTF_8));
        response.setContentType("application/x-msdownload");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        download(response.getOutputStream(), bucketName, fileName);
    }

    @Override
    public void download(OutputStream outputStream, String bucketName, String fileName) throws Exception {
        try (OSSObject ossObject = getClient().getObject(bucketName, fileName);
             InputStream inputStream = ossObject.getObjectContent()) {
            inputStream.transferTo(outputStream);
        } catch (Exception e) {
            log.error("Alibaba OSS download file failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
