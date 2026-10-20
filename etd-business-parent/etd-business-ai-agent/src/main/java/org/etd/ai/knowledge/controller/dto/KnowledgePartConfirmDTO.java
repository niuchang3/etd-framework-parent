package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** 上传分片确认请求；编号、标识和大小均由后端与MinIO核对。 */
@Data
public class KnowledgePartConfirmDTO {
    /** PUT分片响应中的ETag，可携带HTTP双引号。 */
    @NotBlank @Size(max = 255)
    private String etag;
}
