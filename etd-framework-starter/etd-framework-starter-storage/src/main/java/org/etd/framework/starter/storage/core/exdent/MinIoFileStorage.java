package org.etd.framework.starter.storage.core.exdent;


import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.Lists;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.model.UploadResModel;
import org.etd.framework.starter.storage.model.exdent.ByteUploadModel;
import org.etd.framework.starter.storage.model.exdent.InputStreamUpload;
import org.etd.framework.starter.storage.model.exdent.MultipartFileUploadModel;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MinIoFileStorage extends FileStorage<MinioClient> {

    private StorageProperties.MinIo properties;

    public MinIoFileStorage(StorageProperties.MinIo properties) {
        this.properties = properties;
    }


    @Override
    public UploadResModel upload(MultipartFileUploadModel uploadModel) throws Exception {
        InputStreamUpload upload = new InputStreamUpload();
        upload.setInputStream(uploadModel.getFile().getInputStream());
        upload.setFileName(uploadModel.getFile().getOriginalFilename());
        upload.setContentType(uploadModel.getFile().getContentType());
        upload.setBucketName(uploadModel.getBucketName());
        upload.setDirectory(uploadModel.getDirectory());
        return upload(upload);
    }


    @Override
    public UploadResModel upload(InputStreamUpload uploadModel) throws Exception {
        autoMakeBucket(uploadModel.getBucketName());
        try (InputStream inputStream = uploadModel.getInputStream()) {
            String directory = Optional.ofNullable(uploadModel.getDirectory()).orElse("");
            String fileName = directory + SEPARATOR_BAR + generateFileName(uploadModel.getFileName());
            //上传文件到指定目录
            ObjectWriteResponse objectWriteResponse = getClient().putObject(PutObjectArgs.builder()
                    .bucket(uploadModel.getBucketName())
                    .object(fileName)
                    .contentType(uploadModel.getContentType())
                    .stream(inputStream, inputStream.available(), -1)
                    .build());
            String objectUrl = getObjectUrl(uploadModel.getBucketName(), fileName);
            UploadResModel resModel = new UploadResModel();
            resModel.setFileName(fileName);
            resModel.setFileUrl(objectUrl);
            resModel.setExpired(properties.getExpiry());
            return resModel;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public UploadResModel upload(ByteUploadModel uploadModel) throws Exception {
        autoMakeBucket(uploadModel.getBucketName());

        String fileName = uploadModel.getDirectory() + SEPARATOR_BAR + uploadModel.getFileName();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(uploadModel.getFile());
        getClient().putObject(PutObjectArgs.builder().bucket(uploadModel.getBucketName())
                .object(fileName)
                .contentType(uploadModel.getContentType())
                .stream(byteArrayInputStream, byteArrayInputStream.available(), -1)
                .build());

        String objectUrl = getObjectUrl(uploadModel.getBucketName(), fileName);
        UploadResModel resModel = new UploadResModel();
        resModel.setFileName(fileName);
        resModel.setFileUrl(objectUrl);
        resModel.setExpired(properties.getExpiry());
        return resModel;
    }

    @Override
    public void remove(String bucketName, String fileName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        getClient().removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build());
    }

    @Override
    public void remove(String bucketName, List<String> fileNames) throws Exception {
        List<DeleteObject> deleteObjects = Lists.newArrayList();
        for (String objectName : fileNames) {
            deleteObjects.add(new DeleteObject(objectName));
        }
        getClient().removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build());
    }

    /**
     * 检查桶是否存在
     *
     * @param bucketName
     * @return
     */
    public boolean bucketExists(String bucketName) {
        try {
            return getClient().bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 创建桶
     *
     * @param bucketName
     */
    public void makeBucket(String bucketName) {
        try {
            getClient().makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void autoMakeBucket(String bucketName) {
        if (!properties.getAutoCreateBucket()) {
            return;
        }
        if (bucketExists(bucketName)) {
            return;
        }
        makeBucket(bucketName);
    }

    /**
     * @param bucketName
     * @param fileName
     * @return
     * @throws Exception
     */
    @Override
    public String getObjectUrl(String bucketName, String fileName) throws Exception {
        return getClient().getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(fileName)
                        .expiry(properties.getExpiry())
                        .build()
        );
    }

    @Override
    public void download(HttpServletResponse response, String bucketName, String fileName) throws Exception {
        download(response, bucketName, fileName, null);
    }

    @Override
    public void download(HttpServletResponse response, String bucketName, String fileName, String originalFileName) throws Exception {
        String downLoadFileName = originalFileName;
        if (ObjectUtils.isEmpty(downLoadFileName)) {
            downLoadFileName = fileName;
        }
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(downLoadFileName, "UTF-8"));
        response.setContentType("application/x-msdownload");
        response.setCharacterEncoding("utf-8");
        download(response.getOutputStream(), bucketName, fileName);
    }

    @Override
    public void download(OutputStream outputStream, String bucketName, String fileName) throws Exception {
        try (InputStream fileInputStream = getClient().getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build());
             OutputStream outStream = outputStream
        ) {
            IOUtils.copy(fileInputStream, outStream);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


}
