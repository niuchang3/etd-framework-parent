package org.etd.framework.starter.event.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 事件可靠投递内部 Topic 配置测试。
 */
class EventDeliveryAutoConfigurationTest {

    @Test
    void shouldCreateDeliveryTaskStatusAndDeadLetterTopics() {
        EventServerProperties serverProperties = new EventServerProperties();
        serverProperties.getDelivery().setPartitions(16);
        serverProperties.getDelivery().setReplicas(2);

        KafkaAdmin.NewTopics newTopics = new EventDeliveryAutoConfiguration()
                .eventDeliveryInternalTopics(serverProperties);
        Collection<NewTopic> topics = ReflectionTestUtils.invokeMethod(newTopics, "getNewTopics");

        assertThat(topics).extracting(NewTopic::name).containsExactly(
                "etd.event.delivery.task",
                "etd.event.delivery.status",
                "etd.event.delivery.task.dlt");
        assertThat(topics).allSatisfy(topic -> {
            assertThat(topic.numPartitions()).isEqualTo(16);
            assertThat(topic.replicationFactor()).isEqualTo((short) 2);
        });
    }
}
