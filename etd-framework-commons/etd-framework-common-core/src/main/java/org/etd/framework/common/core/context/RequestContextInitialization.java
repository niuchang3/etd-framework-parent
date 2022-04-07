package org.etd.framework.common.core.context;

/**
 * 请求上下文初始化接口
 *
 * @param <E>
 * @author 牛昌
 */
public interface RequestContextInitialization<E> {

    /**
     * 初始化请求上下文
     *
     * @param e
     */
    void initialization(E e);
}
