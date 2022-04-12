package org.etd.framework.common.core.listener;

import org.etd.framework.common.core.spring.SpringContextHelper;
import org.springframework.util.ObjectUtils;

public interface ListenerHandlerFactory {


    default ListenerHandler getListenerHandler(String listenerHandlerCode) {
        ListenerHandler listenerHandler = (ListenerHandler) SpringContextHelper.getBean(listenerHandlerCode);
        if (ObjectUtils.isEmpty(listenerHandler)) {
            throw new RuntimeException("SpringContext上下文中未找到ListenerHandler的实例,ListenerHandlerCode:" + listenerHandlerCode);
        }
        return listenerHandler;
    }
}
