package org.etd.framework.starter.event.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 事件总线发送端配置。
 *
 * @author Young
 */
@Data
@ConfigurationProperties(prefix = "etd.event.bus.client")
public class EventClientProperties {

    /**
     * 新事件默认发送到的总线入口 Topic。
     */
    private String topic = "etd.event.bus";

    /**
     * 是否在应用启动时验证 Kafka 连接和认证。
     */
    private boolean verifyConnectionOnStartup;

    /**
     * 启动连接验证的最大等待时间。
     */
    private Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * 雪花 ID 生成器配置。
     */
    private IdGenerator idGenerator = new IdGenerator();

    /**
     * 事件异步发送线程池配置。
     */
    private Async async = new Async();

    /**
     * 事件 ID 雪花节点配置，同一时刻运行的实例必须使用唯一的节点组合。
     */
    @Data
    public static class IdGenerator {

        /**
         * 工作节点 ID。
         */
        private long workerId = 1;

        /**
         * 数据中心 ID。
         */
        private long datacenterId = 1;
    }

    /**
     * 事件异步发送线程池配置。
     */
    @Data
    public static class Async {

        /**
         * 常驻工作线程数。
         */
        private int corePoolSize = 2;

        /**
         * 最大工作线程数。
         */
        private int maxPoolSize = 8;

        /**
         * 等待发送任务队列容量。
         */
        private int queueCapacity = 1000;
    }
}
