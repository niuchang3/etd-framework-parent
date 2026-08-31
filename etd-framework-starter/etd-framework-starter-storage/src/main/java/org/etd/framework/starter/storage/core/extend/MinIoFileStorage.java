package org.etd.framework.starter.storage.core.extend;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.model.extend.UploadUrlReqModel;
import org.etd.framework.starter.storage.model.extend.UploadUrlResModel;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.util.StringUtils;

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
    public UploadUrlResModel generateUploadUrl(UploadUrlReqModel reqModel) throws Exception {
        try {
            // 构造上传目标文件路径
            String objectName = StringUtils.hasText(reqModel.getFileName())
                    ? buildObjectPath(reqModel.getDirectory(), reqModel.getFileName())
                    : buildObjectPath(reqModel.getDirectory(), generateFileName(reqModel.getOriginalFileName()));

            // 有效期（秒）
            int expiry = reqModel.getExpiry() != null && reqModel.getExpiry() > 0
                    ? reqModel.getExpiry()
                    : properties.getExpiry();

            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(reqModel.getBucketName())
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
}
