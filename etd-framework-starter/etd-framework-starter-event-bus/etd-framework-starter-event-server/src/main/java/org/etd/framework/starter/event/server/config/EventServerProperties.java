package org.etd.framework.starter.event.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事件总线服务端配置，负责定义和创建统一入口 Topic。
 */
@Data
@ConfigurationProperties(prefix = "etd.event.bus.server")
public class EventServerProperties {

    /**
     * 是否由事件总线服务端自动创建入口 Topic。
     */
    private boolean createTopic = true;

    /**
     * 入口 Topic 分区数；生产环境应根据目标吞吐量和消费并发数配置。
     */
    private int partitions = 6;

    /**
     * 入口 Topic 副本数；本地单节点默认使用 1，生产环境通常配置为 3。
     */
    private int replicas = 1;

}
