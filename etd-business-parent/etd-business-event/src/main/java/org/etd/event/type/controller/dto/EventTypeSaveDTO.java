package org.etd.event.type.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 事件类型新增请求。
 */
@Data
public class EventTypeSaveDTO {

    @NotBlank(message = "事件类型编码不能为空")
    @Size(max = 150, message = "事件类型编码不能超过150个字符")
    @Pattern(regexp = "^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+$",
            message = "事件类型编码必须使用小写点分格式")
    private String eventType;

    @NotBlank(message = "事件名称不能为空")
    @Size(max = 150, message = "事件名称不能超过150个字符")
    private String eventName;

    @NotBlank(message = "来源应用不能为空")
    @Size(max = 100, message = "来源应用不能超过100个字符")
    private String sourceApplication;

    @NotNull(message = "事件协议版本不能为空")
    @Min(value = 1, message = "事件协议版本不能小于1")
    private Integer latestVersion;

    @Size(max = 500, message = "事件说明不能超过500个字符")
    private String description;
}
