package org.etd.framework.starter.storage.core.exdent;


import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import io.minio.errors.*;
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
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class AlibabaOSSFileStorage extends FileStorage<OSSClient> {

    private StorageProperties.AlibabaOSS properties;

    public AlibabaOSSFileStorage(StorageProperties.AlibabaOSS properties) {
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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(uploadModel.getContentType());
            PutObjectRequest request = new PutObjectRequest(uploadModel.getBucketName(), fileName, inputStream, metadata);
            getClient().putObject(request);
            UploadResModel resModel = new UploadResModel();
            resModel.setFileName(fileName);
            resModel.setFileUrl(getObjectUrl(uploadModel.getBucketName(), fileName));
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

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(uploadModel.getContentType());
        PutObjectRequest request = new PutObjectRequest(uploadModel.getBucketName(), fileName, byteArrayInputStream, metadata);
        getClient().putObject(request);
        UploadResModel resModel = new UploadResModel();
        resModel.setFileName(fileName);
        resModel.setFileUrl(getObjectUrl(uploadModel.getBucketName(), fileName));
        resModel.setExpired(properties.getExpiry());
        return resModel;
    }

    @Override
    public void remove(String bucketName, String fileName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        getClient().deleteObject(bucketName, fileName);
    }

    @Override
    public void remove(String bucketName, List<String> fileNames) throws Exception {
        List<DeleteObject> deleteObjects = Lists.newArrayList();
        for (String objectName : fileNames) {
            deleteObjects.add(new DeleteObject(objectName));
        }
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        request.setKeys(fileNames);
        getClient().deleteObjects(request);
    }

    /**
     * 检查桶是否存在
     *
     * @param bucketName
     * @return
     */
    public boolean bucketExists(String bucketName) {
        try {
            return getClient().doesBucketExist(bucketName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 创建桶
     *
     * @param bucketName
     */
    public void makeBucket(String bucketName) {
        try {
            getClient().createBucket(bucketName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
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
        Long dateTime = System.currentTimeMillis() + (properties.getExpiry() * 1000);
        URL url = getClient().generatePresignedUrl(bucketName, fileName, new Date(dateTime));
        if (ObjectUtils.isEmpty(url)) {
            return "";
        }
        return url.toString();
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

        try (OSSObject ossObject = getClient().getObject(bucketName, fileName);
             InputStream inputStream = ossObject.getObjectContent();
             OutputStream outStream = outputStream
        ) {
            IOUtils.copy(inputStream, outStream);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }


}
