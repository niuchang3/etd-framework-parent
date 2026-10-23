package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import java.util.List;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;

/** 分片合并请求，不接受客户端指定归属、状态及审计信息。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class KnowledgeChunkMergeDTO {
    /** 当前资料的乐观锁版本。 */
    @NotNull @Min(0)
    private Long version;
    /** 按当前顺序传入的相邻分片ID，至少2个。 */
    @NotNull @Size(min = 2, max = KnowledgeChunkLimits.MAX_MERGE_BATCH)
    private List<@NotNull @Positive Long> chunkIds;
}
