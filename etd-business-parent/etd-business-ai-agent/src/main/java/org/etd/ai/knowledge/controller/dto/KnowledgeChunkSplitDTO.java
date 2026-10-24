package org.etd.ai.knowledge.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;

/** 手动将一片拆成两片，正文可包含尚未保存的人工修改，不接受客户端指定归属或顺序。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class KnowledgeChunkSplitDTO {
    /** 编辑所依据的资料版本，必填且不能为负。 */
    @NotNull @Min(0)
    private Long version;

    /** 第一片完整正文，保留原分片ID；不能全空，最大200000字符。 */
    @NotBlank @Size(max = KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String firstContent;

    /** 第二片完整正文，使用新分片ID并紧接第一片；不能全空，最大200000字符。 */
    @NotBlank @Size(max = KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secondContent;
}
