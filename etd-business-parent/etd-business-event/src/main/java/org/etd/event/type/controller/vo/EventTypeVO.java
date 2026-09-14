package org.etd.event.type.controller.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 事件类型页面响应对象。
 */
@Data
public class EventTypeVO {
    private Long id;
    private Instant createTime;
    private Instant updateTime;
    private Integer version;
    private String eventType;
    private String eventName;
    private String sourceApplication;
    private Integer latestVersion;
    private Boolean enabled;
    private String description;
}
