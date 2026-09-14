package org.etd.framework.event.core.id;

/**
 * 事件唯一标识生成器。
 */
@FunctionalInterface
public interface EventIdGenerator {

    /**
     * 生成事件唯一标识。
     */
    String generate();
}
