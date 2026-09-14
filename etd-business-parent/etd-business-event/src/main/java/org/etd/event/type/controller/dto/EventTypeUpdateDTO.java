package org.etd.event.type.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 事件类型更新请求，通过版本号阻止页面覆盖并发修改。
 */
@Data
public class EventTypeUpdateDTO {

    @Valid
    @NotNull(message = "事件类型内容不能为空")
    private EventTypeSaveDTO content;

    @NotNull(message = "版本号不能为空")
    private Integer version;
}
