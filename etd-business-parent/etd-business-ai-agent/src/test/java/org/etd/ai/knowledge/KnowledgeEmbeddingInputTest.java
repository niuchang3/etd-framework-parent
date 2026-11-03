package org.etd.ai.knowledge;

import java.util.List;
import java.util.Map;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.framework.starter.ai.document.model.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;

import static org.assertj.core.api.Assertions.assertThat;

/** 官方文本格式化与业务白名单的边界：可检索语义进入 Embedding，权威正文和可信身份保持独立。 */
class KnowledgeEmbeddingInputTest {
    @Test
    void formatsSemanticMetadataWithoutLeakingIdentityOrReplacingEditedBody() {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setId(72901L);
        document.setDocumentName("员工手册");
        document.setTenantId(72902L);
        document.setCreateBy(72903L);
        document.setStorageObjectKey("private-storage-object");
        List<String> headings = List.of("休假制度", "年假申请");
        List<String> captions = List.of("审批流程图");
        List<String> refs = List.of("#/texts/private-block");
        List<Integer> pages = List.of(31);
        Map<String, Object> metadata = Map.of("token", "synthetic-private-token");
        DocumentChunk parsed = new DocumentChunk("original.md", 0, "旧标题\n旧正文", "旧正文", 10,
                headings, captions, refs, pages, metadata);
        KnowledgeChunkEntity chunk = KnowledgeChunkConverter.INSTANCE.toEntity(document, parsed);
        chunk.setId(72904L);
        chunk.setContent("人工确认后的正文");
        Document input = KnowledgeChunkConverter.INSTANCE.toEmbeddingDocument(document, chunk);
        String formatted = input.getFormattedContent(MetadataMode.EMBED);
        assertThat(formatted).contains("员工手册", "休假制度", "年假申请", "审批流程图", "人工确认后的正文")
                .doesNotContain("旧正文", "旧标题", "72901", "72902", "72903", "72904", "private-storage-object",
                        "private-block", "synthetic-private-token");
        assertThat(input.getMetadata()).hasSize(3);
        assertThat(input.getText()).isEqualTo("人工确认后的正文");
        assertThat(chunk.getContent()).isEqualTo("人工确认后的正文");
    }
}
