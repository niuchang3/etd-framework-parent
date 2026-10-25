package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;

/** 召回测试只提交问题，检索模式、阈值和数量使用已保存的知识库设置。 */
@Data
public class KnowledgeRecallDTO {
    /** 待检索问题，非空白且最多2000个字符。 */
    @NotBlank
    @Size(max = KnowledgeRetrievalPolicy.MAX_QUERY_CHARACTERS)
    private String query;
}
