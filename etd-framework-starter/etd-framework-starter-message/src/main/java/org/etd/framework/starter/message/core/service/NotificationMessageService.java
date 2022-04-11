package org.etd.framework.starter.message.core.service;

import org.etd.framework.starter.message.core.model.MessageRequest;
import org.etd.framework.starter.message.core.queue.MessageQueue;

/**
 * @author 牛昌
 */
public interface NotificationMessageService {

    /**
     * 发送消息通知
     *
     * @param messageQueue
     * @param messageRequest
     */
    void sendMessage(MessageQueue messageQueue, MessageRequest messageRequest);


}
