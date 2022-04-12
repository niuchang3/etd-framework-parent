package org.etd.framework.common.core.listener;

import org.etd.framework.common.core.model.NotificationMsgRequest;

/**
 * 监听处理器
 */
public interface ListenerHandler<M extends NotificationMsgRequest> {

    /**
     * 调用监听程序 ImportBeanDefinitionRegistrar
     *
     * @param m
     */
    Object invokeProcessHandler(M m) throws Exception;
}
