package org.etd.framework.starter.storage.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * 服务端 MultipartFile 上传请求模型
 *
 * @author Young
 */
@Data
public class MultipartUploadReqModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 存储目录
     */
    private String directory;

    /**
     * MultipartFile 文件对象
     */
    private MultipartFile file;
}
