package org.etd.framework.starter.ai.memory;

import java.util.Map;
import java.util.List;
import org.etd.framework.starter.ai.agent.AgentExecutionRequest;
import org.etd.framework.starter.ai.memory.AgentModelInput;
import org.springframework.ai.document.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.etd.framework.starter.ai.memory.working.AgentWorkingMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgentMemoryManager 统一记忆管理系统测试")
class AgentMemoryManagerTest {

    private final AgentMemoryManager memoryManager = new AgentMemoryManager();

    @Test
    @DisplayName("验证多知识库召回的 RRF 融合去重与有序组装")
    void shouldFuseKnowledgeRankingsAndAssemblePrompt() {
        // 准备两个知识库的召回列表，其中 f1 在两个库都命中
        Document f1 = new Document("doc-1", "片段1：Java 21 虚拟线程规范", Map.of("source", "k1"));
        Document f2 = new Document("doc-2", "片段2：Spring Boot 3.5 自动配置", Map.of("source", "k1"));
        Document f3 = new Document("doc-1", "片段1：Java 21 虚拟线程规范", Map.of("source", "k2")); // 重复片段
        Document f4 = new Document("doc-3", "片段3：RRF 融合去重算法", Map.of("source", "k2"));

        List<List<Document>> groups = List.of(List.of(f1, f2), List.of(f3, f4));

        AgentExecutionRequest request = new AgentExecutionRequest(
                "你是一个资深架构助手。",
                "请介绍虚拟线程和RRF算法",
                List.of(),
                ChatOptions.builder().build(),
                groups,
                4000,
                3,
                true
        );

        AgentModelInput input = memoryManager.assemble(request);

        assertThat(input).isNotNull();
        assertThat(input.references()).hasSize(3);
        // doc-1 应该排在最前面（双路高召回）
        assertThat(input.references().getFirst().getId()).isEqualTo("doc-1");

        List<Message> instructions = input.prompt().getInstructions();
        assertThat(instructions).isNotEmpty();
        assertThat(instructions.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(instructions.getLast().getText()).isEqualTo("请介绍虚拟线程和RRF算法");
    }

    @Test
    @DisplayName("验证短期历史工作记忆的安全倒序截取，且首条历史必须为 UserMessage")
    void shouldTrimHistorySafelyStartingWithUserMessage() {
        // 历史消息：User -> Assistant -> User -> Assistant
        Message u1 = new UserMessage("你好，我是张三");
        Message a1 = new AssistantMessage("你好张三，有什么可以帮你的？");
        Message u2 = new UserMessage("今天天气怎么样？");
        Message a2 = new AssistantMessage("今天晴空万里，温度适宜。");

        List<Message> history = List.of(u1, a1, u2, a2);

        // 设置适当的 Token 预算（只能容纳部分历史）
        AgentExecutionRequest request = new AgentExecutionRequest(
                "你是一个助理",
                "明天会下雨吗？",
                history,
                ChatOptions.builder().build(),
                List.of(),
                200,
                2,
                false
        );

        AgentModelInput input = memoryManager.assemble(request);
        List<Message> instructions = input.prompt().getInstructions();

        // 验证中间的历史消息部分
        // instructions 结构：[SystemMessage, ...History..., UserMessage(Question)]
        List<Message> actualHistory = instructions.subList(1, instructions.size() - 1);
        if (!actualHistory.isEmpty()) {
            assertThat(actualHistory.getFirst().getMessageType()).isEqualTo(MessageType.USER);
        }
    }

    @Test
    @DisplayName("系统提示词和问题直接超出总预算时应抛出异常拦截")
    void shouldThrowExceptionWhenPromptExceedsBudget() {
        AgentExecutionRequest request = new AgentExecutionRequest(
                "这是一个非常非常非常非常非常非常非常非常非常非常非常非常长的系统提示词",
                "这是一个非常非常非常非常非常非常非常非常非常非常非常非常长的问题",
                List.of(),
                ChatOptions.builder().build(),
                List.of(),
                5, // 极低的预算
                2,
                false
        );

        assertThatThrownBy(() -> memoryManager.assemble(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超出输入预算");
    }    @Test
    void historyWindowRetainsWholeToolTurnAndCountsStructuredOutput() {
        UserMessage question = new UserMessage("查询资料");
        AssistantMessage.ToolCall call = new AssistantMessage.ToolCall("id-1", "function", "lookup", "{\"query\":\"资料\"}");
        List<AssistantMessage.ToolCall> calls = List.of(call);
        AssistantMessage decision = AssistantMessage.builder().content("").toolCalls(calls).build();
        ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse("id-1", "lookup", "结果".repeat(300));
        List<ToolResponseMessage.ToolResponse> responses = List.of(response);
        ToolResponseMessage observation = ToolResponseMessage.builder().responses(responses).build();
        AssistantMessage answer = new AssistantMessage("答案");
        List<Message> history = List.of(question, decision, observation, answer);
        AgentWorkingMemory memory = new AgentWorkingMemory();
        assertThat(memory.selectHistory(history, 100)).isEmpty();
        assertThat(memory.selectHistory(history, 5000)).containsExactlyElementsOf(history);
        assertThat(memory.countMessages(history)).isGreaterThan(memory.countTokens(question.getText()) + memory.countTokens(answer.getText()));
    }
    @Test
    void plainHistoryKeepsExistingTokenBudgetBoundary() {
        UserMessage question = new UserMessage("历史问题");
        AssistantMessage answer = new AssistantMessage("历史答案");
        List<Message> history = List.of(question, answer);
        AgentWorkingMemory memory = new AgentWorkingMemory();
        int budget = memory.countTokens(question.getText()) + memory.countTokens(answer.getText());
        assertThat(memory.selectHistory(history, budget)).containsExactlyElementsOf(history);
    }

}
