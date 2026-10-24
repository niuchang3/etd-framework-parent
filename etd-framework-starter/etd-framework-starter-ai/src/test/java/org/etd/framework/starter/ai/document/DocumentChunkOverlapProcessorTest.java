package org.etd.framework.starter.ai.document;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.etd.framework.starter.ai.document.constant.DocumentBlockType;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.model.DocumentBlock;
import org.etd.framework.starter.ai.document.model.DocumentChunk;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;
import org.etd.framework.starter.ai.document.model.DocumentSourceLocation;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException.Reason;
import org.etd.framework.starter.ai.document.parser.DocumentChunkOverlapProcessor;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** 验证重叠只取前片原始正文、Unicode及Token边界、结构保护和来源追踪。 */
class DocumentChunkOverlapProcessorTest {
    private final DocumentChunkOverlapProcessor processor = new DocumentChunkOverlapProcessor();

    /** 默认0关闭，保留原生正文、元数据及原生Token统计，不偷偷改变已存在的生成行为。 */
    @Test
    void leavesDisabledChunksUntouched() {
        var first = chunk(0, "original");
        var input = List.of(first);
        var parameters = parameters(0, null);
        assertThat(processor.apply(input, List.of(), parameters)).isSameAs(input);
    }

    /** 第三片只能携带第二片核心正文，不得累积第一片已经借入的内容。 */
    @Test
    void usesOriginalPreviousChunkAndMergesOnlyItsSources() {
        var chunks = List.of(chunk(0, "Alpha original"), chunk(1, "B"), chunk(2, "C"));
        var blocks = List.of(block(0, "Alpha original", DocumentBlockType.TEXT),
                block(1, "B", DocumentBlockType.TEXT), block(2, "C", DocumentBlockType.TEXT));
        var parameters = parameters(100, null);
        var result = processor.apply(chunks, blocks, parameters);
        assertThat(result.getFirst()).isSameAs(chunks.getFirst());
        assertThat(result.get(1).rawText()).isEqualTo("Alpha original\n\nB");
        assertThat(result.get(2).rawText()).isEqualTo("B\n\nC");
        assertThat(result.get(2).documentItemRefs()).containsExactly("#/texts/1", "#/texts/2");
        assertThat(result.get(2).pageNumbers()).containsExactly(2, 3);
        assertThat(chunks.get(1).rawText()).isEqualTo("B");
    }

    /** 本地分词预算约束追加长度和最终正文，截取中文及emoji时不产生半个代理对。 */
    @Test
    void respectsFinalTokenBudgetWithoutDamagingUnicodeOrCore() {
        String previous = "中文🙂English内容 ".repeat(1000);
        String core = "本片正文必须完整保留";
        var chunks = List.of(chunk(0, previous), chunk(1, core));
        var blocks = List.of(block(0, previous, DocumentBlockType.TEXT), block(1, core, DocumentBlockType.TEXT));
        var parameters = parameters(8, null);
        var result = processor.apply(chunks, blocks, parameters);
        String raw = result.get(1).rawText();
        String prefix = raw.substring(0, raw.length() - core.length() - 2);
        assertThat(DocumentTokenCounter.count(prefix)).isLessThanOrEqualTo(8);
        assertThat(raw).endsWith("\n\n" + core).doesNotContain("�");
        assertThat(Character.isLowSurrogate(prefix.charAt(0))).isFalse();
        var limitedChunks = List.of(chunk(0, "Previous plain context."), chunk(1, core));
        var limitedBlocks = List.of(block(0, "Previous plain context.", DocumentBlockType.TEXT), blocks.get(1));
        var limited = parameters(8, 24);
        var bounded = processor.apply(limitedChunks, limitedBlocks, limited);
        assertThat(bounded).allSatisfy(item -> assertThat(DocumentTokenCounter.count(item.rawText())).isLessThanOrEqualTo(24));
        assertThat(bounded.get(1).rawText()).endsWith(core);
        assertThat(bounded.get(1).tokenCount()).isEqualTo(DocumentTokenCounter.count(bounded.get(1).rawText()));
    }

    /** 最后一个普通文字块很短时只复制该块，不穿过前面的受保护结构。 */
    @Test
    void doesNotBorrowPartialCodeOrTable() {
        var parameters = parameters(100, null);
        for (var type : List.of(DocumentBlockType.CODE, DocumentBlockType.TABLE)) {
            var input = List.of(chunk(0, "protected payload"), chunk(1, "body"));
            var blocks = List.of(block(0, "protected payload", type), block(1, "body", DocumentBlockType.TEXT));
            assertThat(processor.apply(input, blocks, parameters).get(1)).isSameAs(input.get(1));
        }
        var base = chunk(1, "```protected payload```\n\nshort");
        var protectedThenText = new DocumentChunk(base.filename(), base.index(), base.text(), base.rawText(),
                base.tokenCount(), base.headings(), base.captions(), List.of("#/texts/0", "#/texts/1"), base.pageNumbers(), base.metadata());
        var chunks = List.of(protectedThenText, chunk(2, "body"));
        var blocks = List.of(block(0, "protected payload", DocumentBlockType.CODE),
                block(1, "short", DocumentBlockType.TEXT), block(2, "body", DocumentBlockType.TEXT));
        assertThat(processor.apply(chunks, blocks, parameters).get(1).rawText()).isEqualTo("short\n\nbody");
    }

    /** Word/Markdown新增的加粗、空白和硬换行不能使普通文本的重叠被误跳过。 */
    @Test
    void alignsFormattedOrdinaryTextWithoutChangingStoredContent() {
        String[][] examples = {
            {"table caption", "earlier block\n\n**table caption**", "**table caption**"},
            {"first line\nsecond line", "first line  \nsecond line", "first line  \nsecond line"},
            {"English words wrap in source", "English  words\nwrap in source", "English  words\nwrap in source"},
            {"first second third fourth", "`second third`", "`second third`"},
            {"中文格式🙂尾部", "中文**格式**🙂尾部", "中文**格式**🙂尾部"}
        };
        var parameters = parameters(100, null);
        for (String[] example : examples) {
            var input = List.of(chunk(0, example[1]), chunk(1, "body"));
            var blocks = List.of(block(0, example[0], DocumentBlockType.TEXT), block(1, "body", DocumentBlockType.TEXT));
            var result = processor.apply(input, blocks, parameters);
            assertThat(result.get(1).rawText()).isEqualTo(example[2] + "\n\nbody");
            assertThat(result.getFirst().rawText()).isEqualTo(example[1]);
            assertThat(result.get(1).documentItemRefs()).containsExactly("#/texts/0", "#/texts/1");
        }
    }

    /** 对齐仍须能验证来源；不能因放宽Markdown表示差异复制其他块的正文。 */
    @Test
    void keepsUnknownAndProtectedFormattedSourcesExcluded() {
        var parameters = parameters(100, null);
        var input = List.of(chunk(0, "**actual content**"), chunk(1, "body"));
        var unrelated = List.of(block(0, "different content", DocumentBlockType.TEXT));
        assertThat(processor.apply(input, unrelated, parameters).get(1)).isSameAs(input.get(1));
        for (var type : List.of(DocumentBlockType.CODE, DocumentBlockType.TABLE, DocumentBlockType.PICTURE)) {
            var blocks = List.of(block(0, "actual content", type));
            assertThat(processor.apply(input, blocks, parameters).get(1)).isSameAs(input.get(1));
        }
    }

    /** 引擎核心正文已超过最终本地预算时不能截掉用户内容，给出明确可处理原因。 */
    @Test
    void rejectsOversizedCoreInsteadOfDroppingContent() {
        var input = List.of(chunk(0, "word ".repeat(100)));
        var parameters = parameters(8, 24);
        assertThatThrownBy(() -> processor.apply(input, List.of(), parameters))
                .isInstanceOfSatisfying(DocumentChunkException.class, exception ->
                        assertThat(exception.getReason()).isEqualTo(Reason.OVERLAP_CORE_TOO_LARGE));
    }

    /** 空核心不能被重叠上下文伪装成有效分片。 */
    @Test
    void rejectsEmptyCoreInsteadOfReplacingItWithContext() {
        var input = List.of(chunk(0, "previous body"), chunk(1, " "));
        var blocks = List.of(block(0, "previous body", DocumentBlockType.TEXT));
        var parameters = parameters(8, null);
        assertThatThrownBy(() -> processor.apply(input, blocks, parameters))
                .isInstanceOfSatisfying(DocumentChunkException.class, exception ->
                        assertThat(exception.getReason()).isEqualTo(Reason.OVERLAP_CORE_MISSING));
    }

    /** 找不到来源或顺序重启时不猜测上下文，避免跨文档泄露或复制错误结构。 */
    @Test
    void skipsUnknownSourceAndRestartedOrder() {
        var input = List.of(chunk(0, "first"), chunk(1, "second"));
        var parameters = parameters(10, null);
        assertThat(processor.apply(input, List.of(), parameters).get(1)).isSameAs(input.get(1));
        var restarted = List.of(chunk(1, "first"), chunk(0, "second"));
        var blocks = List.of(block(1, "first", DocumentBlockType.TEXT), block(0, "second", DocumentBlockType.TEXT));
        assertThat(processor.apply(restarted, blocks, parameters).get(1)).isSameAs(restarted.get(1));
    }

    /** 短标题之前的普通文本也参与回溯，并继承所有实际复制到的块及页码。 */
    @Test
    void borrowsAcrossConsecutiveOrdinaryBlocks() {
        String raw = "**Useful previous paragraph.**\n\nNext heading";
        var previous = chunkWithRefs(raw, List.of("#/texts/0", "#/texts/1"));
        var input = List.of(previous, chunk(2, "body"));
        var blocks = List.of(block(0, "Useful previous paragraph.", DocumentBlockType.TEXT),
                block(1, "Next heading", DocumentBlockType.TEXT));
        var parameters = parameters(100, null);
        var result = processor.apply(input, blocks, parameters).get(1);
        assertThat(result.rawText()).isEqualTo(raw + "\n\nbody");
        assertThat(result.documentItemRefs()).containsExactly("#/texts/0", "#/texts/1", "#/texts/2");
        assertThat(result.pageNumbers()).containsExactly(1, 2, 3);
    }

    /** 预算只能容纳尾部块时，不应将未复制的早期块误记为来源。 */
    @Test
    void excludesSourcesOutsideSelectedSuffix() {
        String raw = "earlier paragraph with many words and details\n\nshort heading";
        var previous = chunkWithRefs(raw, List.of("#/texts/0", "#/texts/1"));
        var input = List.of(previous, chunk(2, "body"));
        var blocks = List.of(block(0, "earlier paragraph with many words and details", DocumentBlockType.TEXT),
                block(1, "short heading", DocumentBlockType.TEXT));
        var parameters = parameters(4, null);
        var result = processor.apply(input, blocks, parameters).get(1);
        assertThat(result.rawText()).isEqualTo("short heading\n\nbody");
        assertThat(result.documentItemRefs()).containsExactly("#/texts/1", "#/texts/2");
        assertThat(result.pageNumbers()).containsExactly(2, 3);
    }

    /** 预算内优先选择完整中文句子，句号、问号、感叹号及分号都可作为边界。 */
    @Test
    void alignsChineseSentenceBoundariesInsideTokenBudget() {
        for (String delimiter : List.of("。", "？", "！", "；")) {
            String suffix = "完整尾句。";
            String raw = "这是需要略去的很长前文".repeat(10) + delimiter + suffix;
            int budget = DocumentTokenCounter.count(suffix) + 6;
            var result = overlapText(raw, budget);
            assertThat(result).isEqualTo(suffix + "\n\nbody");
        }
    }

    /** 段落优先于段内句界；源文硬换行不能被误判为完整段落。 */
    @Test
    void prefersParagraphBoundaryAndIgnoresHardLineWrap() {
        String raw = "long earlier context ".repeat(20) + "partial sentence. Still previous paragraph.\n\nFinal paragraph.";
        int budget = DocumentTokenCounter.count("sentence. Still previous paragraph.\n\nFinal paragraph.");
        assertThat(overlapText(raw, budget)).isEqualTo("Final paragraph.\n\nbody");
        String wrapped = "long earlier context ".repeat(20) + "partial sentence. Final line\ncontinues here.";
        int wrappedBudget = DocumentTokenCounter.count("sentence. Final line\ncontinues here.");
        assertThat(overlapText(wrapped, wrappedBudget)).isEqualTo("Final line\ncontinues here.\n\nbody");
    }

    /** 英文标点及闭引号后从下个句子开始，小数点不能成为句子边界。 */
    @Test
    void alignsEnglishSentencesAndKeepsDecimalInsideSentence() {
        String raw = "long earlier context ".repeat(20) + "partial sentence!\" Value is 3.14 today.";
        int budget = DocumentTokenCounter.count("sentence!\" Value is 3.14 today.");
        assertThat(overlapText(raw, budget)).isEqualTo("Value is 3.14 today.\n\nbody");
        String decimal = "a".repeat(100) + " value 3.14 remains";
        int decimalBudget = DocumentTokenCounter.count("value 3.14 remains");
        String result = overlapText(decimal, decimalBudget);
        assertThat(result).doesNotStartWith("14 remains").endsWith("3.14 remains\n\nbody");
    }

    /** 没有可用句界的超长句仍按Token截取，不能把非空重叠丢掉或超出上限。 */
    @Test
    void retainsBoundedSuffixWhenNoSentenceFits() {
        String raw = "没有任何句末标点🙂".repeat(100);
        String result = overlapText(raw, 12);
        String prefix = result.substring(0, result.length() - "\n\nbody".length());
        assertThat(prefix).isNotBlank().doesNotContain("�");
        assertThat(DocumentTokenCounter.count(prefix)).isLessThanOrEqualTo(12);
    }

    /** 无法定位的中间来源与图片都必须阻止回溯，不能跳过去复制更早的普通文本。 */
    @Test
    void stopsAtUnknownOrProtectedIntermediateBlock() {
        for (var type : List.of(DocumentBlockType.TABLE, DocumentBlockType.CODE, DocumentBlockType.PICTURE)) {
            String raw = "earlier text\n\nprotected\n\nlast text";
            var previous = chunkWithRefs(raw, List.of("#/texts/0", "#/texts/1", "#/texts/2"));
            var input = List.of(previous, chunk(3, "body"));
            var blocks = List.of(block(0, "earlier text", DocumentBlockType.TEXT),
                    block(1, "protected", type), block(2, "last text", DocumentBlockType.TEXT));
            var parameters = parameters(100, null);
            var result = processor.apply(input, blocks, parameters).get(1);
            assertThat(result.rawText()).isEqualTo("last text\n\nbody");
            assertThat(result.documentItemRefs()).containsExactly("#/texts/2", "#/texts/3");
        }
        var previous = chunkWithRefs("earlier text\n\nunknown\n\nlast text",
                List.of("#/texts/0", "#/texts/missing", "#/texts/2"));
        var input = List.of(previous, chunk(3, "body"));
        var blocks = List.of(block(0, "earlier text", DocumentBlockType.TEXT), block(2, "last text", DocumentBlockType.TEXT));
        var parameters = parameters(100, null);
        assertThat(processor.apply(input, blocks, parameters).get(1).rawText()).isEqualTo("last text\n\nbody");
    }

    /** 末尾只有Markdown闭合标记时，不能把它们误当成下一句的内容。 */
    @Test
    void doesNotSelectOnlyTrailingMarkdownMarkers() {
        String raw = "**" + "很长的一句话没有中间标点".repeat(30) + "。**";
        String result = overlapText(raw, 10);
        String prefix = result.substring(0, result.length() - "\n\nbody".length());
        assertThat(prefix).isNotEqualTo("**").endsWith("。**");
        assertThat(DocumentTokenCounter.count(prefix)).isLessThanOrEqualTo(10);
    }

    private String overlapText(String raw, int budget) {
        var input = List.of(chunk(0, raw), chunk(1, "body"));
        var blocks = List.of(block(0, raw, DocumentBlockType.TEXT), block(1, "body", DocumentBlockType.TEXT));
        var parameters = parameters(budget, null);
        return processor.apply(input, blocks, parameters).get(1).rawText();
    }

    private DocumentChunk chunkWithRefs(String raw, List<String> refs) {
        var original = chunk(0, raw);
        return new DocumentChunk(original.filename(), original.index(), raw, raw, original.tokenCount(),
                original.headings(), original.captions(), refs, original.pageNumbers(), original.metadata());
    }

    private DocumentChunkParameters parameters(int overlap, Integer max) {
        var basic = DocumentChunkParameters.Basic.builder().strategy(max == null ? DocumentChunkStrategy.HIERARCHICAL
                : DocumentChunkStrategy.HYBRID).maxTokens(max).overlapTokens(overlap).build();
        var normal = DocumentChunkParameters.Normal.builder().build();
        var advanced = DocumentChunkParameters.Advanced.builder().documentTimeout(Duration.ofSeconds(90)).build();
        return new DocumentChunkParameters(basic, normal, advanced);
    }

    private DocumentChunk chunk(int index, String content) {
        var refs = List.of("#/texts/" + index);
        var pages = List.of(index + 1);
        var headings = List.of("Heading " + index);
        var captions = List.<String>of();
        var metadata = Map.<String, Object>of();
        return new DocumentChunk("sample.md", index, content, content, DocumentTokenCounter.count(content),
                headings, captions, refs, pages, metadata);
    }

    private DocumentBlock block(int index, String text, DocumentBlockType type) {
        var location = new DocumentSourceLocation(index + 1, null, null, null, null, null, null);
        var locations = List.of(location);
        return new DocumentBlock("#/texts/" + index, null, type, text, null, locations, null);
    }
}
