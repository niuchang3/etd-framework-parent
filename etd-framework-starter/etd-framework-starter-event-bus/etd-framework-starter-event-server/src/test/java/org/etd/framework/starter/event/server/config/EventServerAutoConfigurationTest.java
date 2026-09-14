package org.etd.framework.starter.event.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.etd.framework.starter.event.client.config.EventBusProperties;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 事件总线服务端入口 Topic 配置测试。
 */
class EventServerAutoConfigurationTest {

    @Test
    void shouldCreateConfiguredIngressTopic() {
        EventBusProperties busProperties = new EventBusProperties();
        busProperties.setTopic("etd.event.bus.test");
        EventServerProperties serverProperties = new EventServerProperties();
        serverProperties.setPartitions(12);
        serverProperties.setReplicas(3);

        KafkaAdmin.NewTopics newTopics = new EventServerAutoConfiguration()
                .eventBusTopic(busProperties, serverProperties);
        Collection<NewTopic> topics = ReflectionTestUtils.invokeMethod(newTopics, "getNewTopics");

        assertThat(topics).singleElement().satisfies(topic -> {
            assertThat(topic.name()).isEqualTo("etd.event.bus.test");
            assertThat(topic.numPartitions()).isEqualTo(12);
            assertThat(topic.replicationFactor()).isEqualTo((short) 3);
        });
    }

    @Test
    void shouldRejectInvalidTopicConfiguration() {
        EventBusProperties busProperties = new EventBusProperties();
        EventServerProperties serverProperties = new EventServerProperties();
        serverProperties.setPartitions(0);

        assertThatThrownBy(() -> new EventServerAutoConfiguration()
                .eventBusTopic(busProperties, serverProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("事件总线入口 Topic 分区数必须大于 0");
    }
}
