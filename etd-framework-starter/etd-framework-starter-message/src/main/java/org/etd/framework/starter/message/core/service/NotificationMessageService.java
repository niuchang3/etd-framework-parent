package org.etd.framework.starter.message.core.service;

import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.common.core.queue.MessageQueue;

/**
 * @author 牛昌
 */
public interface NotificationMessageService {

    /**
     * 发送消息通知
     *
     * @param messageQueue
     * @param notificationMsgRequest
     */
    void sendMessage(MessageQueue messageQueue, NotificationMsgRequest notificationMsgRequest);


}
