package org.etd.ai.agent.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.etd.framework.common.core.constants.BasicConstant;

/** Agent独立启停请求，不覆盖草稿、发布配置及基本信息。 */
@Data
public class AgentStatusDTO {
    /** 目标启停状态：0禁用、1启用。 */
    @NotNull
    @Min(BasicConstant.DataStatus.DISABLED_CODE)
    @Max(BasicConstant.DataStatus.ENABLED_CODE)
    private Integer dataStatus;
    /** 页面读取的当前乐观锁版本。 */
    @NotNull
    @Min(0)
    private Long version;
}
