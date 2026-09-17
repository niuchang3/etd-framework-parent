package org.etd.upms.user.consumer;

import lombok.extern.slf4j.Slf4j;
import org.etd.framework.event.core.codec.EventMessageCodec;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.upms.user.constant.SystemUserEventType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 系统用户创建事件 Kafka 消费者。
 * <p>
 * 用于监听 Kafka 事件总线中的消息，筛选并处理 {@link SystemUserEventType#USER_CREATED} 事件。
 */
@Slf4j
@Component
public class UserCreatedEventConsumer {

    private final EventMessageCodec eventMessageCodec;

    /**
     * 构造函数注入事件消息编解码器。
     *
     * @param eventMessageCodec 统一事件消息编解码器
     */
    public UserCreatedEventConsumer(EventMessageCodec eventMessageCodec) {
        this.eventMessageCodec = eventMessageCodec;
    }

    /**
     * 监听 Kafka 消息并处理用户创建事件。
     *
     * @param messageJson 统一事件协议的 JSON 字符串
     */
    @KafkaListener(
            id = "userCreatedEventConsumer",
            topics = "upms.user.created.to.upms.consumer",
            groupId = "upms.user.created.to.upms.consumer.group"
    )
    public void consumeUserCreatedEvent(String messageJson) {
        try {
            EventMessage eventMessage = eventMessageCodec.decode(messageJson);
            if (SystemUserEventType.USER_CREATED.equals(eventMessage.eventType())) {
                log.info("成功接收到用户创建事件: eventId={}, eventType={}, source={}, payload={}",
                        eventMessage.eventId(),
                        eventMessage.eventType(),
                        eventMessage.source(),
                        eventMessage.payload());
            }
        } catch (Exception exception) {
            log.error("解析或处理 Kafka 用户创建事件消息失败: message={}", messageJson, exception);
        }
    }
}
