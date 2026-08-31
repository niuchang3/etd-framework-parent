package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 完成/合并分片任务响应模型
 *
 * @author Young
 */
@Data
public class CompleteMultipartResModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶内最终保存的对象 Key/相对路径
     */
    private String fileName;

    /**
     * 合并成功后的在线访问外链 URL
     */
    private String fileUrl;
}
