package org.etd.framework.starter.ai.document.parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.etd.framework.starter.ai.document.constant.DocumentBlockType;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.constant.DocumentOptionLimits;
import org.etd.framework.starter.ai.document.constant.RegexBoundaryPlacement;
import org.etd.framework.starter.ai.document.model.*;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import org.springframework.util.Assert;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException.Reason;
import java.util.regex.Matcher;
import org.etd.framework.starter.ai.document.model.DocumentChunk;

/** Docling结构化解析后的正则二次切分，保留块引用和页码，不执行向量化。 */
public final class DocumentRegexChunker {
    /** 同一文档块之间的正文间隔。 */
    private static final String SEPARATOR = "\n\n";
    /** 未指定图片占位文本时采用Docling的占位形式。 */
    private static final String IMAGE_PLACEHOLDER = "![IMAGE]";
    /** 正则匹配的最大执行时间，防止灾难回溯长期占用请求线程。 */
    private static final long REGEX_NANOS = 2_000_000_000L;
    /** 单次匹配的字符读取预算，约束病态表达式的计算量。 */
    private static final long REGEX_STEPS = 20_000_000L;

    /** @param filename 原始文件名 @param parsed 完整Docling解析结果 @param parameters 正则策略参数
     * @return 按原文顺序生成的分片草稿 @throws IllegalArgumentException 参数、结构或预算不满足 */
    public List<DocumentChunk> chunk(String filename, DocumentParseResult parsed, DocumentChunkParameters parameters) {
        Assert.isTrue(parameters.basic().strategy() == DocumentChunkStrategy.REGEX, "必须使用正则策略");
        Assert.isTrue(!Boolean.TRUE.equals(parameters.normal().useMarkdownImages()),
                "正则策略当前使用图片占位符，不支持Markdown图片引用");
        String tokenizer = parameters.advanced().tokenizer();
        Assert.isTrue(tokenizer == null || DocumentTokenCounter.TOKENIZER.equals(tokenizer),
                "正则切分目前仅支持cl100k_base分词器");
        List<Span> spans = buildSpans(parsed.blocks(), parameters);
        StringBuilder fullText = new StringBuilder();
        for (Span span : spans) { fullText.append(span.text()); }
        String text = fullText.toString();
        List<Range> ranges = matchRanges(text, spans, parameters);
        List<Range> bounded = boundRanges(text, spans, ranges, parameters.basic().chunkTokenBudget());
        List<DocumentChunk> chunks = createChunkList(filename, text, spans, bounded);
        return mergeSmallChunks(chunks, parameters);
    }

    /** 识别可切分边界，受保护结构及Unicode代理对不得被截断。 */
    private List<Range> matchRanges(String text, List<Span> spans, DocumentChunkParameters parameters) {
        GuardedText guarded = new GuardedText(text);
        Pattern pattern = Pattern.compile(parameters.basic().pattern());
        Matcher matcher = pattern.matcher(guarded);
        List<Range> ranges = new ArrayList<>();
        int start = 0;
        try {
            while (matcher.find()) {
                if (touchesProtected(spans, matcher.start(), matcher.end())) { continue; }
                requireCharacterBoundary(text, matcher.start());
                requireCharacterBoundary(text, matcher.end());
                int end = parameters.normal().boundaryPlacement() == RegexBoundaryPlacement.PREVIOUS
                        ? matcher.end() : matcher.start();
                addRange(ranges, start, end);
                start = parameters.normal().boundaryPlacement() == RegexBoundaryPlacement.DISCARD ? matcher.end() : end;
            }
        } catch (StackOverflowError error) {
            throw new DocumentChunkException(Reason.REGEX_TOO_DEEP);
        }
        addRange(ranges, start, text.length());
        return ranges;
    }

    private void requireCharacterBoundary(String text, int offset) {
        Assert.isTrue(offset <= 0 || offset >= text.length() || !Character.isHighSurrogate(text.charAt(offset - 1))
                || !Character.isLowSurrogate(text.charAt(offset)), "正则边界不能拆开Unicode字符");
    }

    /** 标题、表格和代码按结构生成正文；页码与引用随原始块区间一起保留。 */
    private List<Span> buildSpans(List<DocumentBlock> blocks, DocumentChunkParameters parameters) {
        List<Span> spans = new ArrayList<>();
        List<String> headings = new ArrayList<>();
        int offset = 0;
        for (DocumentBlock block : blocks) {
            if (block.type() == DocumentBlockType.HEADING) { updateHeadings(headings, block); }
            String text = renderBlock(block, parameters);
            if (text == null || text.isBlank()) { continue; }
            String body = text + SEPARATOR;
            List<String> path = List.copyOf(headings);
            boolean protect = block.type() == DocumentBlockType.TABLE || block.type() == DocumentBlockType.CODE;
            Span span = new Span(offset, offset + body.length(), body, block, path, protect);
            spans.add(span);
            offset += body.length();
        }
        return spans;
    }

    private void updateHeadings(List<String> headings, DocumentBlock block) {
        int level = block.headingLevel() == null ? 1 : block.headingLevel();
        while (headings.size() >= level && !headings.isEmpty()) { headings.removeLast(); }
        if (block.text() != null && !block.text().isBlank()) { headings.add(block.text()); }
    }

    private String renderBlock(DocumentBlock block, DocumentChunkParameters parameters) {
        if (block.type() == DocumentBlockType.TABLE && block.tableData() != null) {
            return renderTable(block.tableData(), parameters.basic().useMarkdownTables());
        }
        if (block.type() == DocumentBlockType.PICTURE) {
            String placeholder = parameters.normal().imagePlaceholder();
            return placeholder == null ? IMAGE_PLACEHOLDER : placeholder;
        }
        if (block.type() == DocumentBlockType.CODE && block.text() != null) {
            return "```\n" + block.text() + "\n```";
        }
        if (block.type() == DocumentBlockType.HEADING && block.text() != null) {
            int level = block.headingLevel() == null ? 1 : block.headingLevel();
            return "#".repeat(level) + " " + block.text();
        }
        return block.text();
    }

    private String renderTable(DocumentTable table, boolean markdown) {
        if (!markdown) {
            StringBuilder text = new StringBuilder();
            for (DocumentTableCell cell : table.cells()) {
                text.append("[").append(cell.firstRow()).append(", ").append(cell.firstColumn()).append("] ");
                text.append(cell.text() == null ? "" : cell.text()).append("\n");
            }
            return text.toString();
        }
        if (table.rowCount() == null || table.columnCount() == null || table.rowCount() <= 0 || table.columnCount() <= 0) {
            throw new IllegalArgumentException("表格缺少行列信息，无法生成Markdown表格");
        }
        String[][] values = new String[table.rowCount()][table.columnCount()];
        for (DocumentTableCell cell : table.cells()) {
            if (cell.firstRow() == null || cell.firstColumn() == null) { continue; }
            if (cell.firstRow() >= 0 && cell.firstRow() < values.length
                    && cell.firstColumn() >= 0 && cell.firstColumn() < table.columnCount()) {
                values[cell.firstRow()][cell.firstColumn()] = cell.text();
            }
        }
        return renderMarkdownRows(values);
    }

    private String renderMarkdownRows(String[][] values) {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < values.length; row++) {
            text.append("|");
            for (String value : values[row]) {
                text.append(" ").append(value == null ? "" : value.replace("|", "\\|").replace("\n", "<br>"));
                text.append(" |");
            }
            text.append("\n");
            if (row == 0) {
                text.append("|");
                for (int column = 0; column < values[row].length; column++) { text.append(" --- |"); }
                text.append("\n");
            }
        }
        return text.toString();
    }

    private boolean touchesProtected(List<Span> spans, int start, int end) {
        for (Span span : spans) {
            if (span.start() > end) { break; }
            if (span.protectedBlock() && start > span.start() && start < span.end()) { return true; }
            if (span.protectedBlock() && start < span.end() && end > span.start()) { return true; }
        }
        return false;
    }

    private void addRange(List<Range> ranges, int start, int end) {
        if (end > start) {
            Assert.isTrue(ranges.size() < DocumentOptionLimits.MAX_CHUNK_COUNT, "分片数量超过上限，请调整规则");
            Range range = new Range(start, end);
            ranges.add(range);
        }
    }

    /** 超长片段按Unicode码点安全地截取，表格和代码不从内部截断。 */
    private List<Range> boundRanges(String text, List<Span> spans, List<Range> ranges, Integer maxTokens) {
        if (maxTokens == null) { return ranges; }
        List<Range> result = new ArrayList<>();
        for (Range range : ranges) {
            int start = range.start();
            while (start < range.end()) {
                int end = fittingEnd(text, start, range.end(), maxTokens);
                for (Span span : spans) {
                    if (span.protectedBlock() && end > span.start() && end < span.end()) {
                        if (span.start() > start) { end = span.start(); }
                        else { throw new DocumentChunkException(Reason.PROTECTED_BLOCK_TOO_LARGE); }
                        break;
                    }
                }
                Assert.isTrue(end > start, "Token上限过小，无法保留完整字符");
                addRange(result, start, end);
                start = end;
            }
        }
        return result;
    }

    /** 先指数探测局部边界，再二分收缩；避免每生成一片都重新编码剩余整篇正文。 */
    private int fittingEnd(String text, int start, int end, int limit) {
        int available = end - start;
        int low = 0;
        int high = Math.min(available, limit);
        while (true) {
            int position = advanceCodePoints(text, start, end, high);
            if (DocumentTokenCounter.count(text.substring(start, position)) > limit) { break; }
            if (position == end) { return end; }
            low = high;
            high = Math.min(available, high * 2);
        }
        while (low < high) {
            int middle = (low + high + 1) / 2;
            int position = advanceCodePoints(text, start, end, middle);
            if (DocumentTokenCounter.count(text.substring(start, position)) <= limit) { low = middle; }
            else { high = middle - 1; }
        }
        return advanceCodePoints(text, start, end, low);
    }

    private int advanceCodePoints(String text, int start, int end, int count) {
        int position = start;
        for (int index = 0; index < count && position < end; index++) {
            position += Character.charCount(text.codePointAt(position));
        }
        return position;
    }

    /** 短片段优先向后合并，末片向前合并；拼接正文而非源区间，防止恢复已丢弃的分隔符。 */
    private List<DocumentChunk> mergeSmallChunks(List<DocumentChunk> chunks, DocumentChunkParameters parameters) {
        if (!Boolean.TRUE.equals(parameters.normal().mergeSmallChunks())) { return chunks; }
        List<DocumentChunk> result = new ArrayList<>(chunks);
        int index = 0;
        while (index < result.size()) {
            DocumentChunk current = result.get(index);
            if (current.tokenCount() >= parameters.normal().minTokens()) { index++; continue; }
            int other = index + 1 < result.size() ? index + 1 : index - 1;
            if (other < 0) { break; }
            int first = Math.min(index, other);
            DocumentChunk combined = combineChunks(result.get(first), result.get(first + 1));
            Integer max = parameters.basic().chunkTokenBudget();
            if (max != null && combined.tokenCount() > max) { index++; continue; }
            result.set(first, combined);
            result.remove(first + 1);
            index = first;
        }
        List<DocumentChunk> ordered = new ArrayList<>();
        for (DocumentChunk chunk : result) {
            DocumentChunk item = new DocumentChunk(chunk.filename(), ordered.size(), chunk.text(), chunk.rawText(), chunk.tokenCount(),
                    chunk.headings(), chunk.captions(), chunk.documentItemRefs(), chunk.pageNumbers(), chunk.metadata());
            ordered.add(item);
        }
        return ordered;
    }

    private DocumentChunk combineChunks(DocumentChunk first, DocumentChunk second) {
        String content = first.rawText() + SEPARATOR + second.rawText();
        Set<String> headings = new LinkedHashSet<>(first.headings());
        headings.addAll(second.headings());
        Set<Integer> pages = new LinkedHashSet<>(first.pageNumbers());
        pages.addAll(second.pageNumbers());
        Set<String> refs = new LinkedHashSet<>(first.documentItemRefs());
        refs.addAll(second.documentItemRefs());
        List<String> path = List.copyOf(headings);
        List<Integer> pageNumbers = List.copyOf(pages);
        List<String> references = List.copyOf(refs);
        List<String> captions = List.of();
        Map<String, Object> metadata = Map.of();
        return new DocumentChunk(first.filename(), first.index(), content, content, DocumentTokenCounter.count(content),
                path, captions, references, pageNumbers, metadata);
    }

    private List<DocumentChunk> createChunkList(String filename, String text, List<Span> spans, List<Range> ranges) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (Range range : ranges) {
            String content = text.substring(range.start(), range.end()).strip();
            if (content.isBlank()) { continue; }
            Set<String> headings = new LinkedHashSet<>();
            Set<Integer> pages = new LinkedHashSet<>();
            Set<String> refs = new LinkedHashSet<>();
            for (Span span : spans) {
                if (span.end() <= range.start() || span.start() >= range.end()) { continue; }
                headings.addAll(span.headings());
                if (span.block().id() != null) { refs.add(span.block().id()); }
                for (DocumentSourceLocation location : span.block().locations()) { pages.add(location.page()); }
            }
            List<String> path = List.copyOf(headings);
            List<Integer> pageNumbers = List.copyOf(pages);
            List<String> references = List.copyOf(refs);
            List<String> captions = List.of();
            Map<String, Object> metadata = Map.of();
            DocumentChunk chunk = new DocumentChunk(filename, chunks.size(), content, content, DocumentTokenCounter.count(content),
                    path, captions, references, pageNumbers, metadata);
            chunks.add(chunk);
        }
        return chunks;
    }

    /** 原文输出区间，用于将二次切分结果映射回Docling块。 */
    private record Span(
            /** 原文区间起点，包含该位置。 */
            int start,
            /** 原文区间终点，不包含该位置。 */
            int end,
            /** 该结构块序列化后的正文及段落间隔。 */
            String text,
            /** 原始Docling结构块。 */
            DocumentBlock block,
            /** 该块所属标题路径快照。 */
            List<String> headings,
            /** 是否保护该结构块不被正则及Token切分拆开。 */
            boolean protectedBlock) {}
    /** 采用左闭右开的UTF-16下标，截取时保证不拆Unicode代理对。 */
    private record Range(
            /** 包含的原文起点。 */
            int start,
            /** 不包含的原文终点。 */
            int end) {}

    /** 通过CharSequence读取预算直接中止病态回溯，不创建无法可靠取消的后台线程。 */
    private static final class GuardedText implements CharSequence {
        private final String value;
        private final long deadline = System.nanoTime() + REGEX_NANOS;
        private long remaining = REGEX_STEPS;
        private GuardedText(String value) { this.value = value; }
        @Override public int length() { return value.length(); }
        @Override public char charAt(int index) {
            if (--remaining < 0 || System.nanoTime() > deadline) { throw new DocumentChunkException(Reason.REGEX_BUDGET_EXCEEDED); }
            return value.charAt(index);
        }
        @Override public CharSequence subSequence(int start, int end) { return value.subSequence(start, end); }
        @Override public String toString() { return value; }
    }
}
