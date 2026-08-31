package org.etd.framework.starter.storage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 完成/合并分片任务请求模型
 *
 * @author Young
 */
@Data
public class CompleteMultipartReqModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 存储桶内的对象 Key/相对路径
     */
    private String fileName;

    /**
     * 分片上传任务唯一标识 ID
     */
    private String uploadId;

    /**
     * 已成功上传的所有分片 ETag 列表
     */
    private List<PartETagInfo> parts;

    /**
     * 单个分片的 ETag 校验信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartETagInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 分片序号（1, 2, 3...）
         */
        private Integer partNumber;

        /**
         * 该分片直传成功后云端返回的 ETag 校验码
         */
        private String eTag;
    }
}
