package org.etd.framework.starter.event.server.delivery.retry;

import org.etd.framework.starter.event.server.config.EventServerProperties;

import java.time.Duration;

/**
 * 根据当前失败次数计算指数退避时间。
 */
public class EventDeliveryRetryPolicy {

    private final Duration initialBackoff;
    private final double multiplier;
    private final Duration maxBackoff;

    public EventDeliveryRetryPolicy(EventServerProperties.Delivery properties) {
        initialBackoff = properties.getInitialBackoff();
        multiplier = properties.getBackoffMultiplier();
        maxBackoff = properties.getMaxBackoff();
    }

    /**
     * 计算当前失败后、下一次投递前的等待时间。
     *
     * @param deliveryAttempt 当前投递次数，从 1 开始
     * @return 不超过最大退避时间的等待时长
     */
    public Duration calculateRetryBackoff(int deliveryAttempt) {
        double calculated = initialBackoff.toMillis()
                * Math.pow(multiplier, Math.max(0, deliveryAttempt - 1));
        long delayMillis = Math.min((long) calculated, maxBackoff.toMillis());
        return Duration.ofMillis(delayMillis);
    }
}
