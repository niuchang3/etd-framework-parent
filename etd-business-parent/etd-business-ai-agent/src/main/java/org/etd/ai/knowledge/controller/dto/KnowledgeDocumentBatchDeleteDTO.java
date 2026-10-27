package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import org.etd.ai.knowledge.constant.KnowledgePolicy;

/** 当前知识库内的显式多选删除请求，不支持按筛选条件隐式删除全部资料。 */
@Data
public class KnowledgeDocumentBatchDeleteDTO {
    /** 页面当前知识库ID，防止混入其他知识库的选择项。 */
    @NotNull
    @Positive
    private Long knowledgeBaseId;

    /** 待删除的资料ID，1至100项，不允许空元素、非正ID或重复ID。 */
    @NotEmpty
    @Size(max = KnowledgePolicy.MAX_BATCH_DELETE)
    private List<@NotNull @Positive Long> documentIds;
}
