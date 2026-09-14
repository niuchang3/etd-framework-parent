package org.etd.event.type.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 事件类型页面响应对象。
 */
@Data
public class EventTypeVO {

    /** 事件类型主键。 */
    private Long id;

    /** 事件类型的创建时间。 */
    private Instant createTime;

    /** 事件类型的最后修改时间。 */
    private Instant updateTime;

    /** 事件类型的乐观锁版本号。 */
    private Integer version;

    /** 全局唯一且创建后不可修改的事件类型编码。 */
    private String eventType;

    /** 事件类型的显示名称。 */
    private String eventName;

    /** 允许发送该事件的来源应用标识。 */
    private String sourceApplication;

    /** 当前事件协议的最新版本号。 */
    private Integer latestVersion;

    /** 是否允许继续接收该类型的事件。 */
    private Boolean enabled;

    /** 事件类型的业务语义说明。 */
    private String description;
}
