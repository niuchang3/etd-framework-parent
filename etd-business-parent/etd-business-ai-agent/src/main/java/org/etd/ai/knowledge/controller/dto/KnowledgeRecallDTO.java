package org.etd.ai.knowledge.controller.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
/** 召回问题及测试状态筛选，不允许客户端任意切换模型和物理表。 */
@Data
public class KnowledgeRecallDTO {
    /** 测试问题，最多2000字符。 */
    @NotBlank @Size(max = 2000)
    private String query;
    /** 测试时可按启停筛选，正式接口忽略并强制启用。 */
    @Min(0) @Max(1)
    private Integer dataStatus;
}
