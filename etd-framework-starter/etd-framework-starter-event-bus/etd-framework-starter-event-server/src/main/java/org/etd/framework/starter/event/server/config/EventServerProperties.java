package org.etd.framework.starter.event.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

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

    /**
     * Kafka 内部投递闭环配置。
     */
    private Delivery delivery = new Delivery();

    /**
     * 内部投递任务、状态投影和失败重试配置。
     */
    @Data
    public static class Delivery {

        /** 内部投递任务 Topic。 */
        private String taskTopic = "etd.event.delivery.task";

        /** 内部投递状态 Topic。 */
        private String statusTopic = "etd.event.delivery.status";

        /** 自动重试耗尽后的死信 Topic。 */
        private String deadLetterTopic = "etd.event.delivery.task.dlt";

        /** 多实例共享的投递任务消费组。 */
        private String taskConsumerGroup = "etd-event-delivery-dispatcher";

        /** 多实例共享的状态投影消费组。 */
        private String statusConsumerGroup = "etd-event-delivery-status-projector";

        /** 内部 Topic 分区数。 */
        private int partitions = 16;

        /** 内部 Topic 副本数。 */
        private int replicas = 1;

        /** 包含首次发送在内的最大投递次数。 */
        private int maxAttempts = 4;

        /** 首次失败后的退避时间。 */
        private Duration initialBackoff = Duration.ofSeconds(1);

        /** 后续重试退避倍数。 */
        private double backoffMultiplier = 2.0D;

        /** 单次退避允许达到的最大时间。 */
        private Duration maxBackoff = Duration.ofSeconds(30);
    }

}
