package org.etd.framework.starter.event.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事件总线客户端配置。
 *
 * @author Young
 */
@Data
@ConfigurationProperties(prefix = "etd.event.bus")
public class EventBusProperties {

    /**
     * 总线统一 Topic 名称。
     */
    private String topic = "etd.event.bus";

    /**
     * Topic 分区数量。
     */
    private int partitions = 6;

    /**
     * Topic 副本数量，本地单节点 Kafka 默认使用一个副本。
     */
    private int replicas = 1;
}
