package org.etd.framework.starter.event.client.core.connection;

import org.apache.kafka.clients.admin.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.KafkaAdmin;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 应用启动时的 Kafka 连接验证器，用于尽早暴露地址或 SASL 认证配置错误。
 *
 * @author Young
 */
public class KafkaConnectionVerifier implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnectionVerifier.class);

    private final KafkaAdmin kafkaAdmin;
    private final Duration connectionTimeout;

    public KafkaConnectionVerifier(KafkaAdmin kafkaAdmin, Duration connectionTimeout) {
        this.kafkaAdmin = kafkaAdmin;
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * 查询 Kafka 集群标识，以同时验证网络连通性和认证配置。
     */
    @Override
    public void run(ApplicationArguments args) {
        validateConnectionTimeout();
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            String clusterId = admin.describeCluster().clusterId()
                    .get(connectionTimeout.toMillis(), TimeUnit.MILLISECONDS);
            LOGGER.info("Kafka 连接验证成功，集群标识：{}", clusterId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka 启动连接验证被中断", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Kafka 连接或认证失败，请检查 spring.kafka 配置", exception);
        }
    }

    private void validateConnectionTimeout() {
        if (connectionTimeout == null || connectionTimeout.isZero() || connectionTimeout.isNegative()) {
            throw new IllegalArgumentException("Kafka 连接验证超时时间必须大于 0");
        }
    }
}
