package org.etd.framework.starter.ai.memory.semantic;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.springframework.ai.document.Document;

/** 官方 FunctionToolCallback 的数据能力：范围绑定当前 Run，模型仅能提供检索词。 */
public final class AgentKnowledgeTool implements Function<AgentKnowledgeTool.Input, String> {
    /** 内置知识工具名称，不能被业务注册的同名工具覆盖。 */
    public static final String NAME = "search_knowledge";
    private final List<Long> baseIds;
    private final AgentKnowledgeQueryService knowledge;
    private final AgentRunControl control;
    private final int maximum;
    private final int budget;
    private final AgentSemanticMemory memory = new AgentSemanticMemory();
    private final List<Document> references = new ArrayList<>();

    /** 仅允许模型生成查询词，不声明 tenantId/userId/知识库范围参数。 */
    public record Input(@JsonPropertyDescription("需要检索的完整问题或关键词") String query) { }

    /** @param baseIds 可信绑定范围 @param knowledge 授权召回 @param control Run 控制 @param maximum 引用上限 @param budget 工具返回预算 */
    public AgentKnowledgeTool(List<Long> baseIds, AgentKnowledgeQueryService knowledge, AgentRunControl control,
            int maximum, int budget) {
        this.baseIds = List.copyOf(baseIds);
        this.knowledge = knowledge;
        this.control = control;
        this.maximum = maximum;
        this.budget = budget;
    }

    @Override
    public String apply(Input input) {
        control.requireActive();
        if (input.query() == null || input.query().isBlank()) {
            throw new IllegalArgumentException("知识检索词不能为空");
        }
        List<List<Document>> groups = memory.queryKnowledgeList(baseIds, input.query(), knowledge);
        List<Document> ranked = memory.fuseRankings(groups);
        List<Document> fresh = new ArrayList<>();
        for (Document fragment : ranked) {
            if (!containsReference(fragment.getId())) {
                fresh.add(fragment);
            }
        }
        StringBuilder text = new StringBuilder();
        memory.appendKnowledge(fresh, maximum, references, text, budget);
        control.requireActive();
        List<Document> selected = List.copyOf(references);
        control.recordReferences(selected);
        if (text.isEmpty()) {
            return "未找到可追加的授权知识片段，请根据已有信息回答并明确证据限制。";
        }
        return text.toString();
    }

    private boolean containsReference(String id) {
        for (Document fragment : references) {
            if (fragment.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
