package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 申请生成带时效性 GET 对象访问/下载 URL 请求模型
 *
 * @author Young
 */
@Data
public class GenerateObjectUrlReqModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 数据库保存的对象 Key/相对路径
     */
    private String fileName;

    /**
     * 链接有效时长（单位：秒；可选，不填默认使用配置的 expiry）
     */
    private Integer expiry;
}
