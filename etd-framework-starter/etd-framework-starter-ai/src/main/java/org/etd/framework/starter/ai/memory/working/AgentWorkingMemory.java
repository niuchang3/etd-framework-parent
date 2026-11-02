package org.etd.framework.starter.ai.memory.working;

import java.util.ArrayList;
import java.util.List;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * 短期工作记忆：负责历史消息流的Token预算估算裁剪与因果安全对齐。
 * 确保消息流不超Token预算，并且上下文始终以用户的提问作为起始边界。
 */
public final class AgentWorkingMemory {
    private static final int MESSAGE_OVERHEAD = 16;

    /**
     * 根据剩余输入Token预算，从最近到最远倒序裁剪历史，并剔除开头的孤立助手消息。
     * @param history 历史消息流
     * @param remainingBudget 剩余可用Token预算
     * @return 裁剪后可安全输入大模型的消息列表
     */
    public List<Message> selectHistory(List<Message> history, int remainingBudget) {
        if (history == null || history.isEmpty() || remainingBudget <= 0) {
            return List.of();
        }
        List<Message> selected = new ArrayList<>();
        int end = history.size();
        int budget = remainingBudget;
        while (end > 0) {
            int start = end - 1;
            while (start >= 0 && !(history.get(start) instanceof UserMessage)) {
                start--;
            }
            if (start < 0) {
                break;
            }
            List<Message> turn = history.subList(start, end);
            int cost = countMessages(turn);
            if (cost > budget) {
                break;
            }
            selected.addAll(0, turn);
            budget -= cost;
            end = start;
        }
        return List.copyOf(selected);
    }

    /** 预算包含结构化工具参数与结果，不能只统计 Message.getText()。 */
    public int countMessages(List<Message> messages) {
        int count = 0;
        for (Message message : messages) {
            // 普通文本沿用既有每条消息开销；空正文的工具消息仍需计入角色和结构开销。
            count += Math.max(countTokens(message.getText()), MESSAGE_OVERHEAD);
            if (message instanceof AssistantMessage assistant) {
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    count += countTokens(call.id()) + countTokens(call.name()) + countTokens(call.arguments());
                }
            }
            if (message instanceof ToolResponseMessage tool) {
                for (ToolResponseMessage.ToolResponse response : tool.getResponses()) {
                    count += countTokens(response.id()) + countTokens(response.name()) + countTokens(response.responseData());
                }
            }
        }
        return count;
    }

    public int countTokens(String text) {
        return text == null || text.isEmpty() ? 0 : DocumentTokenCounter.count(text) + MESSAGE_OVERHEAD;
    }
}
