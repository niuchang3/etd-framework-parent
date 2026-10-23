package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import java.util.List;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;

/** 分片修改保存请求，不接受客户端指定归属、状态及审计信息。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class KnowledgeChunkSaveDTO {
    /** 当前资料的乐观锁版本。 */
    @NotNull @Min(0)
    private Long version;
    /** 本次修改的分片，按需提交，不要求传入全部分片。 */
    @NotNull @Size(min = 1, max = KnowledgeChunkLimits.MAX_EDIT_BATCH)
    private List<@NotNull @Valid KnowledgeChunkEditDTO> chunks;
}
