package org.etd.framework.starter.storage.core.extend;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO 文件存储扩展实现
 *
 * @author Young
 */
@Slf4j
public class MinIoFileStorage extends FileStorage<MinioClient> {

    private final StorageProperties.MinIo properties;

    public MinIoFileStorage(StorageProperties.MinIo properties) {
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
            
            // 修复大文件截断 BUG：根据物理文件大小正确配置 objectSize 与 partSize
            long objectSize = uploadModel.getFileSize() > 0 ? uploadModel.getFileSize() : -1;
            long partSize = objectSize > 0 ? -1 : 10 * 1024 * 1024; // 10MB 分片上传

            getClient().putObject(PutObjectArgs.builder()
                    .bucket(uploadModel.getBucketName())
                    .object(fileName)
                    .contentType(uploadModel.getContentType())
                    .stream(inputStream, objectSize, partSize)
                    .build());

            String objectUrl = getObjectUrl(uploadModel.getBucketName(), fileName);
            UploadResModel resModel = new UploadResModel();
            resModel.setFileName(fileName);
            resModel.setFileUrl(objectUrl);
            resModel.setExpired(properties.getExpiry());
            return resModel;
        } catch (Exception e) {
            log.error("MinIO upload file failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public UploadResModel upload(ByteUploadModel uploadModel) throws Exception {
        autoMakeBucket(uploadModel.getBucketName());

        String fileName = buildObjectPath(uploadModel.getDirectory(), generateFileName(uploadModel.getFileName()));
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(uploadModel.getFile());

        getClient().putObject(PutObjectArgs.builder()
                .bucket(uploadModel.getBucketName())
                .object(fileName)
                .contentType(uploadModel.getContentType())
                .stream(byteArrayInputStream, uploadModel.getFile().length, -1)
                .build());

        String objectUrl = getObjectUrl(uploadModel.getBucketName(), fileName);
        UploadResModel resModel = new UploadResModel();
        resModel.setFileName(fileName);
        resModel.setFileUrl(objectUrl);
        resModel.setExpired(properties.getExpiry());
        return resModel;
    }

    @Override
    public void remove(String bucketName, String fileName) throws Exception {
        try {
            getClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("MinIO remove file failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void remove(String bucketName, List<String> fileNames) throws Exception {
        try {
            List<DeleteObject> deleteObjects = new ArrayList<>();
            for (String objectName : fileNames) {
                deleteObjects.add(new DeleteObject(objectName));
            }
            getClient().removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build());
        } catch (Exception e) {
            log.error("MinIO batch remove files failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean bucketExists(String bucketName) {
        try {
            return getClient().bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("MinIO check bucketExists failed: {}", e.getMessage(), e);
        }
        return false;
    }

    public void makeBucket(String bucketName) {
        try {
            getClient().makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("MinIO makeBucket failed: {}", e.getMessage(), e);
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
        try {
            return getClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(properties.getExpiry())
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO getObjectUrl failed: {}", e.getMessage(), e);
            throw e;
        }
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
        try (InputStream fileInputStream = getClient().getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build())) {
            fileInputStream.transferTo(outputStream);
        } catch (Exception e) {
            log.error("MinIO download file failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
