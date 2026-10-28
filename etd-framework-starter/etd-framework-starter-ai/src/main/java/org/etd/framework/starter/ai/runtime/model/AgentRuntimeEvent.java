package org.etd.framework.starter.ai.runtime.model;

import java.util.Map;

/** 统一运行输出，与HTTP响应及业务实体解耦。
 * @param type 事件类型
 * @param sequence 同轮递增序号
 * @param progress 标识、引用及用量，累计正文仅终态事件填充
 * @param text 本次文本增量，非增量事件为空
 * @param result 终态事件的实际执行结果，其他事件为空
 * @param attributes 调用方提供的关联标识，框架不解析其业务含义
 */
public record AgentRuntimeEvent(AgentRuntimeEventType type, long sequence, AgentRunProgress progress,
        String text, AgentRuntimeResult result, Map<String, Object> attributes) {
}
