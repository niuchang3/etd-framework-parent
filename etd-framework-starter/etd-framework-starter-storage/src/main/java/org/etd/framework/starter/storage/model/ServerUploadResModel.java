package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务端文件上传响应模型
 *
 * @author Young
 */
@Data
public class ServerUploadResModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶内最终保存的对象 Key/相对路径
     */
    private String fileName;

    /**
     * 文件在线访问外链 URL
     */
    private String fileUrl;
}
