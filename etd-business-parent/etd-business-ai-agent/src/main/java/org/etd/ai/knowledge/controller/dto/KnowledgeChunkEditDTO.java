package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;

/** 分片正文编辑输入，不接受客户端指定归属、状态及审计信息。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class KnowledgeChunkEditDTO {
    /** 目标分片ID。 */
    @NotNull @Positive
    private Long id;
    /** 修改后的完整正文，不允许全空。 */
    @NotBlank @Size(max = KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String content;
}
