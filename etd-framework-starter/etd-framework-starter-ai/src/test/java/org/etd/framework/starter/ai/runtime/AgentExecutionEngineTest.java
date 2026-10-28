package org.etd.framework.starter.ai.runtime;

import java.util.List;
import java.util.Map;
import org.etd.framework.starter.ai.runtime.model.AgentExecutionRequest;
import org.etd.framework.starter.ai.runtime.model.KnowledgeFragment;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.junit.jupiter.api.Test;
import org.etd.framework.starter.ai.runtime.model.AgentModelInput;

import static org.assertj.core.api.Assertions.*;

/** 验证跨库融合、引用预算和历史完整问答，不连接模型。 */
class AgentExecutionEngineTest {
    private final AgentExecutionEngine engine = new AgentExecutionEngine();

    /** 同一分片跨库出现时合并排名，只发送一次正文，引用顺序与上下文编号一致。 */
    @Test
    void fusesRankingsAndDeduplicatesWithReferenceLimit() {
        KnowledgeFragment first = fragment("1", "设备安装说明");
        KnowledgeFragment shared = fragment("2", "共同证据");
        KnowledgeFragment third = fragment("3", "另一库的说明");
        AgentKnowledgeQueryService knowledgeService = query -> List.of(List.of(first, shared), List.of(third, shared));
        AgentExecutionRequest request = request(knowledgeService, List.of(), 1000, 2);
        AgentModelInput prepared = engine.prepare(request);
        assertThat(prepared.references()).extracting(KnowledgeFragment::id).containsExactly("2", "1");
        String content = prepared.prompt().getContents();
        assertThat(content).contains("[1]\n共同证据", "[2]\n设备安装说明").doesNotContain("另一库的说明");
    }

    /** 超大分片不挤爆输入预算，超大的系统提示词则明确失败。 */
    @Test
    void keepsOversizedEvidenceOutAndRejectsOversizedRequiredInput() {
        KnowledgeFragment large = fragment("1", "知识内容".repeat(2000));
        AgentKnowledgeQueryService knowledgeService = query -> List.of(List.of(large));
        AgentExecutionRequest input = request(knowledgeService, List.of(), 256, 12);
        assertThat(engine.prepare(input).references()).isEmpty();
        ChatOptions options = ChatOptions.builder().build();
        AgentExecutionRequest invalid = new AgentExecutionRequest("系统指令".repeat(500), "问题", List.of(), options, knowledgeService, 256, 12, true);
        assertThatThrownBy(() -> engine.prepare(invalid)).hasMessageContaining("超出输入预算");
    }

    /** 历史按完整问答对保留且最近一轮优先。 */
    @Test
    void keepsRecentHistoryPairsTogether() {
        UserMessage oldQuestion = new UserMessage("旧问题".repeat(1000));
        AssistantMessage oldAnswer = new AssistantMessage("旧答案".repeat(1000));
        UserMessage recentQuestion = new UserMessage("最近问题");
        AssistantMessage recentAnswer = new AssistantMessage("最近答案");
        List<Message> history = List.of(oldQuestion, oldAnswer, recentQuestion, recentAnswer);
        AgentKnowledgeQueryService knowledgeService = query -> List.of();
        AgentExecutionRequest request = request(knowledgeService, history, 512, 12);
        String content = engine.prepare(request).prompt().getContents();
        assertThat(content).contains("最近问题", "最近答案").doesNotContain("旧问题", "旧答案");
    }

    private KnowledgeFragment fragment(String id, String content) {
        return new KnowledgeFragment(id, content, Map.of());
    }

    private AgentExecutionRequest request(AgentKnowledgeQueryService knowledgeService, List<Message> history, int budget, int count) {
        ChatOptions options = ChatOptions.builder().maxTokens(100).build();
        return new AgentExecutionRequest("帮助用户", "如何安装", history, options, knowledgeService, budget, count, true);
    }
}
