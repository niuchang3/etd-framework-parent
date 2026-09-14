package org.etd.framework.event.core.sender;

import java.util.Objects;

/**
 * 事件发送完成后的传输无关结果。
 *
 * @param eventId    事件唯一标识
 * @param destination 实际发送目的地
 */
public record EventSendResult(String eventId, String destination) {

    public EventSendResult {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("事件 ID 不能为空");
        }
        destination = Objects.requireNonNull(destination, "事件发送目的地不能为空");
    }
}
