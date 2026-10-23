package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;

/** 分片生成请求，不接受客户端指定归属、状态及审计信息。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class KnowledgeChunkGenerateDTO {
    /** 当前资料的乐观锁版本。 */
    @NotNull @Min(0)
    private Long version;
    /** 三级分片参数，页面参数与后端统一契约一致。 */
    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private DocumentChunkParameters parameters;
}
