package org.etd.framework.starter.ai.runtime.model;

import java.util.Map;
import java.util.function.Supplier;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.etd.framework.starter.ai.runtime.spi.AgentRunEventListener;
import org.springframework.ai.chat.model.ChatModel;

/** Agent执行请求：业务提供聊天输入、模型、运行记录服务和事件接收器。
 * @param executionId 唯一执行标识
 * @param chatRequest 已授权聊天输入及检索插槽
 * @param model 本轮授权模型供应器，在实际调用前执行
 * @param recordService 运行记录读写插槽
 * @param eventListener 运行事件接收插槽
 * @param attributes 业务关联标识，不允许存放凭证
 */
public record AgentRunRequest(String executionId, AgentExecutionRequest chatRequest, Supplier<ChatModel> model,
        AgentRunRecordService recordService, AgentRunEventListener eventListener, Map<String, Object> attributes) {
    public AgentRunRequest {
        attributes = Map.copyOf(attributes);
    }
}
