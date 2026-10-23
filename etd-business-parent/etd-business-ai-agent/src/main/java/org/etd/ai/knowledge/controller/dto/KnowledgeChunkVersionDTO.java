package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

/** 分片确认请求，不接受客户端指定归属、状态及审计信息。 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class KnowledgeChunkVersionDTO {
    /** 当前资料的乐观锁版本。 */
    @NotNull @Min(0)
    private Long version;
}
