package org.etd.ai.agent.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 发布请求，明确携带页面当前版本以拒绝发布他人刚修改的草稿。 */
@Data
public class AgentVersionDTO {
    /** 页面读取的非负乐观锁版本。 */
    @NotNull
    @Min(0)
    private Long version;
}
