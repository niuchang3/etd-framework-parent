package org.etd.framework.starter.storage.core.extend;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.model.extend.UploadUrlReqModel;
import org.etd.framework.starter.storage.model.extend.UploadUrlResModel;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.Date;

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
}
