package org.etd.framework.starter.event.server.eventbus.model;

import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 业务订阅投递目标快照。
 *
 * @param subscriptionId 订阅配置标识
 * @param targetTopic 订阅消息目标 Topic
 */
public record EventSubscriptionTarget(Long subscriptionId, String targetTopic) {

    public EventSubscriptionTarget {
        Objects.requireNonNull(subscriptionId, "订阅配置标识不能为空");
        Assert.hasText(targetTopic, "订阅目标 Topic 不能为空");
    }
}
