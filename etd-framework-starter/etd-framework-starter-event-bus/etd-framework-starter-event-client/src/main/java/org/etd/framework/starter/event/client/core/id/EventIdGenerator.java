package org.etd.framework.starter.event.client.core.id;

/**
 * 事件唯一标识生成器。
 */
@FunctionalInterface
public interface EventIdGenerator {

    /**
     * 生成事件唯一标识。
     *
     * @return 事件唯一标识
     */
    String generate();
}
