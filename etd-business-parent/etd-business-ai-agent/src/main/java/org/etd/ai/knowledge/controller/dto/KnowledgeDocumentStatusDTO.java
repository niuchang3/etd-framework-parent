package org.etd.ai.knowledge.controller.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 文档正式检索启停请求，不能改变上传和分片状态。 */
@Data
public class KnowledgeDocumentStatusDTO {
    /** 当前文档版本。 */
    @NotNull @Min(0)
    private Long version;
    /** 0禁用、1启用。 */
    @NotNull @Min(0) @Max(1)
    private Integer dataStatus;
}
