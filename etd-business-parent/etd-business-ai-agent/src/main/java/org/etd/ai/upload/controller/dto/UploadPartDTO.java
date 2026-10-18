package org.etd.ai.upload.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** UploadPartDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class UploadPartDTO {
    /** 从1开始的分片序号。 */
    @NotNull
    @Min(1)
    @Max(10000)
    private Integer partNumber;

    /** MinIO返回的不透明ETag。 */
    @NotBlank
    @Size(max = 200)
    private String etag;
}
