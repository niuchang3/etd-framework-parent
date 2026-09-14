package org.etd.event.constant;

/**
 * 事件运行表固定分片约定，前后端使用同一范围校验路由参数。
 */
public final class EventShardingConstant {

    /** 第一版固定物理分片数量。 */
    public static final int SHARD_COUNT = 16;

    /** 合法分片编号上限，分片编号从零开始。 */
    public static final int MAX_SHARDING_KEY = SHARD_COUNT - 1;

    private EventShardingConstant() {
    }
}
