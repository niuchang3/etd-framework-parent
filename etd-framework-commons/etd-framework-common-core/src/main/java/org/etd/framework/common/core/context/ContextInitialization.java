package org.etd.framework.common.core.context;

/**
 * 上下文初始化
 *
 * @param <E>
 * @author 牛昌
 */
public interface ContextInitialization<E> {


    /**
     * 初始化请求上下文
     *
     * @param e
     */
    void initialization(E e);

}
