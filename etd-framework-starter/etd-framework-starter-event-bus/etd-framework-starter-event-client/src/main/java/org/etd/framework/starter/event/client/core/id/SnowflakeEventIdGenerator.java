package org.etd.framework.starter.event.client.core.id;

import cn.hutool.core.lang.Snowflake;

/**
 * 基于雪花算法的事件唯一标识生成器。
 */
public class SnowflakeEventIdGenerator implements EventIdGenerator {

    private final Snowflake snowflake;

    public SnowflakeEventIdGenerator(long workerId, long datacenterId) {
        this.snowflake = new Snowflake(workerId, datacenterId);
    }

    @Override
    public String generate() {
        // 字符串可避免跨语言传输时超过 JavaScript 的安全整数范围。
        return Long.toString(snowflake.nextId());
    }
}
