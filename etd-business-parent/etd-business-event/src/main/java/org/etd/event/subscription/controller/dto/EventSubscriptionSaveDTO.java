package org.etd.event.subscription.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 事件订阅新增请求。
 */
@Data
public class EventSubscriptionSaveDTO {

    /** 订阅配置在管理页面展示的名称。 */
    @NotBlank(message = "订阅名称不能为空")
    @Size(max = 150, message = "订阅名称不能超过150个字符")
    private String subscriptionName;

    /** 需要接收的事件类型主键。 */
    @NotNull(message = "事件类型不能为空")
    private Long eventTypeId;

    /** 实际处理该事件的业务应用标识。 */
    @NotBlank(message = "订阅应用不能为空")
    @Size(max = 100, message = "订阅应用不能超过100个字符")
    private String subscriberApplication;

    /** Event Server 发布订阅事件的目标 Kafka Topic。 */
    @NotBlank(message = "目标 Topic 不能为空")
    @Size(max = 250, message = "目标 Topic 不能超过250个字符")
    private String targetTopic;

    /** 订阅的业务用途或消费逻辑说明。 */
    @Size(max = 500, message = "订阅说明不能超过500个字符")
    private String description;
}
