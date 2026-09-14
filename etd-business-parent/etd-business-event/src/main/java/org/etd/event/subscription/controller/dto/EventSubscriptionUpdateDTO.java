package org.etd.event.subscription.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 事件订阅更新请求，通过版本号阻止覆盖并发修改。
 */
@Data
public class EventSubscriptionUpdateDTO {

    @Valid
    @NotNull(message = "订阅内容不能为空")
    private EventSubscriptionSaveDTO content;

    @NotNull(message = "版本号不能为空")
    private Integer version;
}
