package org.etd.ai.model.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.etd.framework.common.core.constants.BasicConstant;

/** 连接启停请求，不要求重传服务地址或API Key。 */
@Data
public class ModelConnectionStatusDTO {
    /** 目标状态：0停用、1启用，保存配置不等于验证连通性。 */
    @NotNull
    @Min(BasicConstant.DataStatus.DISABLED_CODE)
    @Max(BasicConstant.DataStatus.ENABLED_CODE)
    private Integer dataStatus;

    /** 详情返回的当前非负版本，用于拒绝并发覆盖。 */
    @NotNull
    @Min(0)
    private Long version;
}
