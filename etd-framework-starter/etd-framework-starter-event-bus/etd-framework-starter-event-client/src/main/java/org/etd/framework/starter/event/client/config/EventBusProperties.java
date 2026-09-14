package org.etd.framework.starter.event.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事件总线公共配置，确保发送端与服务端使用同一个入口 Topic。
 */
@Data
@ConfigurationProperties(prefix = "etd.event.bus")
public class EventBusProperties {

    /**
     * 事件总线统一入口 Topic，业务发送端不得自行指定其他入口。
     */
    private String topic = "etd.event.bus";
}
