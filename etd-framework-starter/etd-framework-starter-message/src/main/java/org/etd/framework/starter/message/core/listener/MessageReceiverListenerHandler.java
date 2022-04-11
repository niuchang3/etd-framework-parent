package org.etd.framework.starter.message.core.listener;

import org.etd.framework.starter.message.core.model.MessageRequest;

import java.io.IOException;

/**
 * @author 牛昌
 * @description
 * @date 2020/9/7
 */
public interface MessageReceiverListenerHandler {

    /**
     * 调用进程处理程序
     *
     * @param messageRequest
     * @throws IOException
     */
    void invokeProcessHandler(MessageRequest messageRequest) throws IOException;
}
