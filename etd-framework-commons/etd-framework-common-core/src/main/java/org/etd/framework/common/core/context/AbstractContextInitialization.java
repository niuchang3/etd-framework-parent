package org.etd.framework.common.core.context;

public abstract class AbstractContextInitialization<E> implements ContextInitialization<E> {


    @Override
    public void initialization(E e) {
        beforeInitialization(e);
        invoke(e);
        afterInitialization(e);
    }


    /**
     * 初始化之前调用
     *
     * @param e
     */
    public abstract void beforeInitialization(E e);


    /**
     * 初始化之后调用
     *
     * @param e
     */
    public abstract void afterInitialization(E e);

    /**
     * 调用
     * @param e
     */
    public abstract void invoke(E e);
}
