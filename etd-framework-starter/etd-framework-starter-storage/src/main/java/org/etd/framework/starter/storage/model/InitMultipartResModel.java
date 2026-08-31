package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 初始化分片任务响应模型
 *
 * @author Young
 */
@Data
public class InitMultipartResModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分片上传任务唯一标识 ID
     */
    private String uploadId;

    /**
     * 存储桶内最终保存的对象 Key/相对路径
     */
    private String fileName;
}
