package org.etd.framework.starter.ai.document;

import java.time.Duration;
import java.util.List;
import org.etd.framework.starter.ai.document.constant.*;
import org.etd.framework.starter.ai.document.model.*;
import org.etd.framework.starter.ai.document.parser.DocumentRegexChunker;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException.Reason;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.etd.framework.starter.ai.document.model.DocumentBlock;
import org.etd.framework.starter.ai.document.model.DocumentChunk;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;
import org.etd.framework.starter.ai.document.model.DocumentParseResult;
import org.etd.framework.starter.ai.document.model.DocumentSourceLocation;
import org.etd.framework.starter.ai.document.model.DocumentTable;
import org.etd.framework.starter.ai.document.model.DocumentTableCell;
import static org.assertj.core.api.Assertions.*;

/** 验证正则边界、Unicode、结构保护及来源追踪，不依赖外部模型。 */
class DocumentRegexChunkerTest {
    private final DocumentRegexChunker chunker = new DocumentRegexChunker();

    /** 标题边界归入下一片，拆分后页码和块引用仍对应正文。 */
    @Test
    void splitsHeadingsAndPreservesSource() {
        DocumentBlock first = block("#/texts/0", DocumentBlockType.HEADING, "First", 1, 1);
        DocumentBlock body = block("#/texts/1", DocumentBlockType.TEXT, "First content", null, 1);
        DocumentBlock second = block("#/texts/2", DocumentBlockType.HEADING, "Second", 1, 2);
        DocumentBlock tail = block("#/texts/3", DocumentBlockType.TEXT, "Second content", null, 2);
        DocumentParseResult result = parsed(List.of(first, body, second, tail));
        DocumentChunkParameters options = parameters("(?m)^# ", RegexBoundaryPlacement.NEXT, null, false, null);
        List<DocumentChunk> chunks = chunker.chunk("sample.pdf", result, options);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst().rawText()).startsWith("# First");
        assertThat(chunks.getLast().rawText()).startsWith("# Second");
        assertThat(chunks.getLast().pageNumbers()).containsExactly(2);
        assertThat(chunks.getLast().documentItemRefs()).containsExactly("#/texts/2", "#/texts/3");
    }

    /** 三种边界位置各自保留或丢弃匹配文本，短片段合并不恢复被丢弃内容。 */
    @ParameterizedTest
    @EnumSource(RegexBoundaryPlacement.class)
    void respectsBoundaryPlacement(RegexBoundaryPlacement placement) {
        DocumentBlock block = block("#/texts/0", DocumentBlockType.TEXT, "alpha---beta---gamma", null, 1);
        DocumentParseResult result = parsed(List.of(block));
        DocumentChunkParameters options = parameters("---", placement, null, false, null);
        List<DocumentChunk> chunks = chunker.chunk("sample.pdf", result, options);
        assertThat(chunks).hasSize(3);
        if (placement == RegexBoundaryPlacement.NEXT) { assertThat(chunks.get(1).rawText()).isEqualTo("---beta"); }
        if (placement == RegexBoundaryPlacement.PREVIOUS) { assertThat(chunks.getFirst().rawText()).isEqualTo("alpha---"); }
        if (placement == RegexBoundaryPlacement.DISCARD) {
            assertThat(chunks.get(1).rawText()).isEqualTo("beta");
            DocumentChunkParameters merging = parameters("---", placement, 100, true, 50);
            List<DocumentChunk> merged = chunker.chunk("sample.pdf", result, merging);
            assertThat(merged).hasSize(1);
            assertThat(merged.getFirst().rawText()).doesNotContain("---").contains("alpha", "beta", "gamma");
        }
    }

    /** 超长正文分割不损坏中文和代理对，每片本地Token计数均在上限内。 */
    @Test
    void splitsLongUnicodeWithoutCorruption() {
        String text = "中文🙂测试内容".repeat(30);
        DocumentBlock block = block("#/texts/0", DocumentBlockType.TEXT, text, null, 1);
        DocumentParseResult result = parsed(List.of(block));
        DocumentChunkParameters options = parameters("NEVER_MATCH", RegexBoundaryPlacement.NEXT, 30, false, null);
        List<DocumentChunk> chunks = chunker.chunk("sample.pdf", result, options);
        StringBuilder joined = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            assertThat(DocumentTokenCounter.count(chunk.rawText())).isLessThanOrEqualTo(30);
            assertThat(chunk.rawText()).doesNotContain("�");
            joined.append(chunk.rawText());
        }
        assertThat(joined.toString()).isEqualTo(text);
    }

    /** 代码内部匹配不得切开代码块；原子结构超长时明确要求提高上限。 */
    @Test
    void protectsCodeAndRejectsOversizedAtomicContent() {
        DocumentBlock block = block("#/texts/0", DocumentBlockType.CODE, "first---second", null, 1);
        DocumentParseResult result = parsed(List.of(block));
        DocumentChunkParameters options = parameters("---", RegexBoundaryPlacement.DISCARD, null, false, null);
        assertThat(chunker.chunk("sample.pdf", result, options).getFirst().rawText()).contains("first---second");
        DocumentChunkParameters small = parameters("---", RegexBoundaryPlacement.DISCARD, 1, false, null);
        assertThatThrownBy(() -> chunker.chunk("sample.pdf", result, small))
                .isInstanceOfSatisfying(DocumentChunkException.class, exception ->
                        assertThat(exception.getReason()).isEqualTo(Reason.PROTECTED_BLOCK_TOO_LARGE))
                .hasMessageContaining("提高上限");
    }

    /** 规范条款正则可运行；代码整体超过512时准确提示，提高到1024后成功且正文不丢失。 */
    @Test
    void supportsRuleRegexAndReportsAtomicTokenLimit() {
        String code = "public void test() { int value = 123; }\n".repeat(45);
        DocumentBlock rule = block("#/texts/0", DocumentBlockType.LIST_ITEM, "1. 【强制】第一条规则", null, 1);
        DocumentBlock example = block("#/texts/1", DocumentBlockType.CODE, code, null, 1);
        DocumentBlock next = block("#/texts/2", DocumentBlockType.LIST_ITEM, "2. 【推荐】第二条规则", null, 2);
        DocumentParseResult result = parsed(List.of(rule, example, next));
        String pattern = "(?m)^\\h*(?:[-*+]\\h+)?[0-9]+[.．、]\\h*【(?:强制|推荐|参考)】";
        DocumentChunkParameters small = parameters(pattern, RegexBoundaryPlacement.NEXT, 512, false, null);
        assertThatThrownBy(() -> chunker.chunk("sample.pdf", result, small))
                .isInstanceOfSatisfying(DocumentChunkException.class, exception ->
                        assertThat(exception.getReason()).isEqualTo(Reason.PROTECTED_BLOCK_TOO_LARGE));
        DocumentChunkParameters large = parameters(pattern, RegexBoundaryPlacement.NEXT, 1024, false, null);
        List<DocumentChunk> chunks = chunker.chunk("sample.pdf", result, large);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst().rawText()).contains(code);
        assertThat(chunks.getLast().rawText()).startsWith("2. 【推荐】");
    }

    /** 表格按行列输出Markdown，正则匹配表格行时不能破坏原子结构。 */
    @Test
    void preservesMarkdownTableRowsAndHeaders() {
        DocumentTableCell header = new DocumentTableCell("Header", 0, 1, 0, 1, true, false);
        DocumentTableCell value = new DocumentTableCell("Value", 1, 2, 0, 1, false, false);
        List<DocumentTableCell> cells = List.of(header, value);
        DocumentTable table = new DocumentTable(2, 1, cells);
        List<DocumentSourceLocation> locations = List.of();
        DocumentBlock block = new DocumentBlock("#/tables/0", null, DocumentBlockType.TABLE, null, null, locations, table);
        DocumentParseResult result = parsed(List.of(block));
        DocumentChunkParameters.Basic basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.REGEX)
                .pattern("(?m)^\\|").useMarkdownTables(true).build();
        DocumentChunkParameters.Normal normal = DocumentChunkParameters.Normal.builder().boundaryPlacement(RegexBoundaryPlacement.DISCARD).build();
        DocumentChunkParameters.Advanced advanced = DocumentChunkParameters.Advanced.builder().documentTimeout(Duration.ofSeconds(30)).build();
        DocumentChunkParameters options = new DocumentChunkParameters(basic, normal, advanced);
        List<DocumentChunk> chunks = chunker.chunk("sample.pdf", result, options);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().rawText()).contains("| Header |\n| --- |\n| Value |");
    }

    /** 病态回溯按读取预算停止，不会创建难以取消的执行线程。 */
    @Test
    void stopsPathologicalRegexBacktracking() {
        DocumentBlock block = block("#/texts/0", DocumentBlockType.TEXT, "a".repeat(10000) + "!", null, 1);
        DocumentParseResult result = parsed(List.of(block));
        DocumentChunkParameters options = parameters("(a+)+$", RegexBoundaryPlacement.NEXT, null, false, null);
        assertThatThrownBy(() -> chunker.chunk("sample.pdf", result, options))
                .isInstanceOf(DocumentChunkException.class).hasMessageContaining("正则");
    }

    private DocumentChunkParameters parameters(String pattern, RegexBoundaryPlacement placement, Integer max,
            boolean merge, Integer min) {
        DocumentChunkParameters.Basic basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.REGEX)
                .pattern(pattern).maxTokens(max).build();
        DocumentChunkParameters.Normal normal = DocumentChunkParameters.Normal.builder().boundaryPlacement(placement)
                .mergeSmallChunks(merge).minTokens(min).build();
        Duration timeout = Duration.ofSeconds(30);
        DocumentChunkParameters.Advanced advanced = DocumentChunkParameters.Advanced.builder().documentTimeout(timeout).build();
        return new DocumentChunkParameters(basic, normal, advanced);
    }
    private DocumentBlock block(String id, DocumentBlockType type, String text, Integer level, int page) {
        DocumentSourceLocation location = new DocumentSourceLocation(page, null, null, null, null, null, null);
        List<DocumentSourceLocation> locations = List.of(location);
        return new DocumentBlock(id, null, type, text, level, locations, null);
    }
    private DocumentParseResult parsed(List<DocumentBlock> blocks) {
        List<String> warnings = List.of();
        return new DocumentParseResult(DocumentParseStatus.SUCCESS, null, blocks, null, null, warnings);
    }
}
