package org.etd.event.subscription.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 事件订阅新增请求。
 */
@Data
public class EventSubscriptionSaveDTO {

    @NotBlank(message = "订阅编码不能为空")
    @Size(max = 100, message = "订阅编码不能超过100个字符")
    @Pattern(regexp = "^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+$",
            message = "订阅编码必须使用小写点分格式")
    private String subscriptionCode;

    @NotBlank(message = "订阅名称不能为空")
    @Size(max = 150, message = "订阅名称不能超过150个字符")
    private String subscriptionName;

    @NotNull(message = "事件类型不能为空")
    private Long eventTypeId;

    @NotBlank(message = "订阅应用不能为空")
    @Size(max = 100, message = "订阅应用不能超过100个字符")
    private String subscriberApplication;

    @NotBlank(message = "目标 Topic 不能为空")
    @Size(max = 250, message = "目标 Topic 不能超过250个字符")
    private String targetTopic;

    @NotBlank(message = "消费组不能为空")
    @Size(max = 150, message = "消费组不能超过150个字符")
    private String consumerGroup;

    @Size(max = 500, message = "订阅说明不能超过500个字符")
    private String description;
}
