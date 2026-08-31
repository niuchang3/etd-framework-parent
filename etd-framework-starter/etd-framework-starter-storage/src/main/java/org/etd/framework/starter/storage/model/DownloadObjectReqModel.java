package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务端物理下载对象请求模型
 *
 * @author Young
 */
@Data
public class DownloadObjectReqModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 数据库保存的对象 Key/相对路径
     */
    private String fileName;
}
