package org.etd.framework.starter.ai.runtime.model;

import java.util.List;
import org.springframework.ai.chat.prompt.Prompt;

/** 完成预算控制的本轮模型输入。
 * @param prompt 发送给模型的消息
 * @param references 实际纳入上下文的引用，顺序对应从1开始的编号
 */
public record AgentModelInput(Prompt prompt, List<KnowledgeFragment> references) {
}
