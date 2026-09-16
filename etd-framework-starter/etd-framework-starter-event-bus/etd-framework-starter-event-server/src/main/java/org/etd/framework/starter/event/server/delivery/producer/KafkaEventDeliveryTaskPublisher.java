package org.etd.framework.starter.event.server.delivery.producer;

import org.etd.framework.starter.event.server.delivery.codec.EventDeliveryCodec;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryTask;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 将稳定投递任务写入 Event Server 内部 Kafka Topic。
 */
public class KafkaEventDeliveryTaskPublisher implements EventDeliveryTaskPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final EventDeliveryCodec deliveryCodec;
    private final String taskTopic;

    public KafkaEventDeliveryTaskPublisher(KafkaTemplate<Object, Object> kafkaTemplate,
                                           EventDeliveryCodec deliveryCodec,
                                           String taskTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.deliveryCodec = deliveryCodec;
        this.taskTopic = taskTopic;
    }

    @Override
    public void publishDeliveryTaskList(List<EventDeliveryTask> taskList) {
        if (!kafkaTemplate.inTransaction()) {
            kafkaTemplate.executeInTransaction(operations -> {
                sendTaskList(operations, taskList);
                return null;
            });
            return;
        }
        sendTaskList(kafkaTemplate, taskList);
    }

    private void sendTaskList(KafkaOperations<Object, Object> operations,
                              List<EventDeliveryTask> taskList) {
        List<CompletableFuture<?>> futures = new ArrayList<>(taskList.size());
        for (EventDeliveryTask task : taskList) {
            futures.add(sendTask(operations, task));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    @Override
    public void publishDeliveryTask(EventDeliveryTask task) {
        publishDeliveryTaskList(List.of(task));
    }

    private CompletableFuture<?> sendTask(KafkaOperations<Object, Object> operations,
                                          EventDeliveryTask task) {
        String key = task.deliveryId().toString();
        return operations.send(taskTopic, key, deliveryCodec.encodeTask(task));
    }
}
