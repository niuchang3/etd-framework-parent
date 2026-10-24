package org.etd.framework.starter.ai.document.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.ai.document.constant.DocumentBlockType;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException;
import org.etd.framework.starter.ai.document.exception.DocumentChunkException.Reason;
import org.etd.framework.starter.ai.document.model.DocumentBlock;
import org.etd.framework.starter.ai.document.model.DocumentChunk;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;

/** 自动生成结果的重叠处理：只读取原始前片，保留正文和来源，不参与人工编辑流程。 */
@Slf4j
public final class DocumentChunkOverlapProcessor {
    /** 重叠上下文与本片核心正文之间的段落间隔。 */
    private static final String SEPARATOR = "\n\n";

    /**
     * @param chunks 尚未追加重叠的单份文档分片，保持输入顺序
     * @param blocks Docling原始结构，用于限制重叠不跨入表格、代码及其他来源块
     * @param parameters 原始用户参数，Token上限包含追加的重叠正文
     * @return 第一片正文不变，其余片最多追加指定Token数的前片安全尾部；0时原样返回
     * @throws DocumentChunkException 核心正文已超过本地计数的最终上限时，不能通过删除正文补救
     */
    public List<DocumentChunk> apply(List<DocumentChunk> chunks, List<DocumentBlock> blocks,
            DocumentChunkParameters parameters) {
        int requested = parameters.basic().overlapTokens();
        if (requested == 0 || chunks.isEmpty()) {
            return chunks;
        }
        var sourceById = indexBlocksById(blocks);
        List<DocumentChunk> result = new ArrayList<>();
        int applied = 0;
        for (int index = 0; index < chunks.size(); index++) {
            var current = chunks.get(index);
            var previous = index == 0 ? null : chunks.get(index - 1);
            var overlapped = applyToChunk(current, previous, sourceById, parameters);
            if (overlapped != current) {
                applied++;
            }
            result.add(overlapped);
        }
        log.info("文档分片重叠处理完成，分片数={}，最多重叠Token数={}，已追加重叠分片数={}，跳过边界数={}",
                result.size(), requested, applied, result.size() - 1 - applied);
        return List.copyOf(result);
    }

    private DocumentChunk applyToChunk(DocumentChunk current, DocumentChunk previous,
            Map<String, DocumentBlock> sourceById, DocumentChunkParameters parameters) {
        Integer limit = parameters.basic().maxTokens();
        if (current.rawText() == null || current.rawText().isBlank()) {
            throw new DocumentChunkException(Reason.OVERLAP_CORE_MISSING);
        }
        int coreTokens = DocumentTokenCounter.count(current.rawText());
        if (limit != null && coreTokens > limit) {
            throw new DocumentChunkException(Reason.OVERLAP_CORE_TOO_LARGE);
        }
        int budget = resolveOverlapBudget(parameters.basic().overlapTokens(), limit, coreTokens);
        var tail = budget == 0 ? null : resolveSourceTail(previous, current, sourceById, budget);
        String prefix = tail == null ? "" : selectPrefix(tail.text(), current.rawText(), budget, limit);
        return prefix.isEmpty() ? current : prependOverlap(current, previous, tail, prefix);
    }

    private Map<String, DocumentBlock> indexBlocksById(List<DocumentBlock> blocks) {
        Map<String, DocumentBlock> result = new HashMap<>();
        for (DocumentBlock block : blocks) {
            if (block.id() != null) {
                result.put(block.id(), block);
            }
        }
        return result;
    }

    /** 从前片末尾逐块验证连续普通文本，遇到受保护、未知或无法对齐的来源立即停止。 */
    private SourceTail resolveSourceTail(DocumentChunk previous, DocumentChunk current,
            Map<String, DocumentBlock> sourceById, int budget) {
        if (previous == null || !Objects.equals(previous.filename(), current.filename())
                || previous.index() >= current.index() || previous.rawText() == null
                || previous.documentItemRefs().isEmpty()) {
            return null;
        }
        String raw = previous.rawText().strip();
        String remaining = raw;
        List<SourceSpan> spans = new ArrayList<>();
        int start = raw.length();
        var refs = previous.documentItemRefs();
        for (int index = refs.size() - 1; index >= 0 && !remaining.isEmpty(); index--) {
            var block = sourceById.get(refs.get(index));
            String matched = resolveBlockTail(remaining, block);
            if (matched == null) {
                break;
            }
            start = remaining.length() - matched.length();
            var span = new SourceSpan(block, remaining.length());
            spans.addFirst(span);
            // 候选已覆盖Token窗口后无需继续扫描更早的来源，避免大文档反复对齐无关正文。
            if (DocumentTokenCounter.count(raw.substring(start)) >= budget) {
                break;
            }
            remaining = remaining.substring(0, start).stripTrailing();
        }
        return spans.isEmpty() ? null : new SourceTail(raw.substring(start), start, List.copyOf(spans));
    }

    /** 不使用来源文字重新拼接正文，保留Docling原始Markdown及各块之间的分隔符。 */
    private String resolveBlockTail(String raw, DocumentBlock block) {
        if (block == null || block.type() == DocumentBlockType.CODE || block.type() == DocumentBlockType.TABLE
                || block.type() == DocumentBlockType.PICTURE || block.text() == null || block.text().isBlank()) {
            return null;
        }
        String source = block.text().strip();
        if (raw.endsWith(source)) {
            return source;
        }
        if (source.contains(raw) && !raw.isBlank()) {
            return raw;
        }
        return resolveFormattedTail(raw, source);
    }

    /** Markdown加粗、内联标记和换行空格只用于对齐；返回的始终是原始正文，不改写格式或内容。 */
    private String resolveFormattedTail(String raw, String source) {
        var normalizedRaw = normalizeForAlignment(raw);
        String normalizedSource = normalizeForAlignment(source).value();
        if (normalizedSource.isEmpty() || normalizedRaw.value().isEmpty()) {
            return null;
        }
        if (normalizedRaw.value().endsWith(normalizedSource)) {
            int alignedIndex = normalizedRaw.value().length() - normalizedSource.length();
            int start = normalizedRaw.offsets()[alignedIndex];
            while (start > 0 && isInlineMarker(raw.charAt(start - 1))) {
                start--;
            }
            return raw.substring(start);
        }
        if (normalizedSource.contains(normalizedRaw.value())) {
            return raw;
        }
        // 超长原始块可能被拆开，仅选择可在来源中验证的最后一段，不越过前面的结构边界。
        int lastBreak = raw.lastIndexOf(SEPARATOR);
        String paragraph = raw.substring(lastBreak < 0 ? 0 : lastBreak + SEPARATOR.length());
        String normalizedParagraph = normalizeForAlignment(paragraph).value();
        return !normalizedParagraph.isEmpty() && normalizedSource.contains(normalizedParagraph)
                ? paragraph : null;
    }

    /** 忽略Docling序列化新增的内联标记和空白，并保存每个UTF-16字符在原文中的位置。 */
    private AlignedText normalizeForAlignment(String text) {
        var value = new StringBuilder(text.length());
        int[] offsets = new int[text.length()];
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (Character.isWhitespace(character) || Character.isSpaceChar(character) || isInlineMarker(character)) {
                continue;
            }
            offsets[value.length()] = index;
            value.append(character);
        }
        return new AlignedText(value.toString(), offsets);
    }

    private boolean isInlineMarker(char character) {
        return character == '*' || character == '_' || character == '`' || character == '~';
    }

    /** 仅在当前调用内使用的文本对齐索引，不作为最终正文或持久化内容。 */
    private record AlignedText(
            /** 去除空白和内联标记后的对齐文本。 */
            String value,
            /** 对齐文本各UTF-16字符在原始字符串中的偏移，未使用的尾部元素不参与查询。 */
            int[] offsets) {}

    private int resolveOverlapBudget(int requested, Integer limit, int coreTokens) {
        if (limit == null) {
            return requested;
        }
        int remaining = limit - coreTokens - DocumentTokenCounter.count(SEPARATOR);
        return Math.min(requested, Math.max(remaining, 0));
    }

    /** 指数探测并二分选择Unicode完整的后缀，所有计数均为cl100k_base而非字符数。 */
    private String selectPrefix(String text, String core, int budget, Integer limit) {
        if (budget == 0 || text.isEmpty()) {
            return "";
        }
        int points = text.codePointCount(0, text.length());
        int low = 0;
        int high = Math.min(points, budget);
        while (DocumentTokenCounter.count(tailByCodePoints(text, high)) <= budget) {
            low = high;
            if (high == points) {
                break;
            }
            high = (int) Math.min(points, (long) high * 2);
        }
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (DocumentTokenCounter.count(tailByCodePoints(text, middle)) <= budget) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        String prefix = tailByCodePoints(text, low).strip();
        prefix = trimToFinalBudget(prefix, core, budget, limit);
        return alignToTextBoundary(text, prefix, core, budget, limit);
    }

    /** 在预算允许的后缀中向右寻找句首：优先完整段落，其次句末标点，长句无边界时保留原截取。 */
    private String alignToTextBoundary(String text, String prefix, String core, int budget, Integer limit) {
        while (!prefix.isEmpty()) {
            int start = text.length() - prefix.length();
            int aligned = findTextBoundary(text, start);
            String candidate = text.substring(aligned).strip();
            String bounded = trimToFinalBudget(candidate, core, budget, limit);
            if (bounded.equals(candidate)) {
                return candidate;
            }
            // BPE拼接复核若缩短了候选，则重新对齐，不能把新的半句直接作为最终结果。
            prefix = bounded;
        }
        return prefix;
    }

    private int findTextBoundary(String text, int start) {
        if (start == 0 || isNaturalStart(text, start)) {
            return start;
        }
        for (int index = start; index < text.length(); index++) {
            if (text.startsWith(SEPARATOR, index) && !text.substring(index).isBlank()) {
                return skipBoundarySuffix(text, index);
            }
        }
        for (int index = start; index < text.length(); index++) {
            if (isSentenceEnd(text, index)) {
                int next = skipBoundarySuffix(text, index + 1);
                if (hasFollowingText(text, next)) {
                    return next;
                }
            }
        }
        return start;
    }

    /** 普通硬换行可能只是PDF折行；只将空行视为段落，英文句点须与小数和域名区分。 */
    private boolean isNaturalStart(String text, int start) {
        int previous = start - 1;
        while (previous >= 0 && (Character.isWhitespace(text.charAt(previous))
                || isClosingQuote(text.charAt(previous)))) {
            previous--;
        }
        return previous < 0 || text.substring(previous + 1, start).contains(SEPARATOR)
                || isSentenceEnd(text, previous);
    }

    private boolean isSentenceEnd(String text, int index) {
        char value = text.charAt(index);
        if ("。！？；!?;".indexOf(value) >= 0) {
            return true;
        }
        return value == '.' && (index + 1 == text.length()
                || Character.isWhitespace(text.charAt(index + 1)) || isClosingQuote(text.charAt(index + 1)));
    }

    private int skipBoundarySuffix(String text, int index) {
        while (index < text.length() && (Character.isWhitespace(text.charAt(index))
                || isClosingQuote(text.charAt(index)))) {
            index++;
        }
        return index;
    }

    /** 句末闭合的Markdown标记不构成下一句话，不能生成只有星号等标记的重叠。 */
    private boolean hasFollowingText(String text, int start) {
        for (int index = start; index < text.length(); index++) {
            char value = text.charAt(index);
            if (!Character.isWhitespace(value) && !isInlineMarker(value) && !isClosingQuote(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClosingQuote(char value) {
        return "”’\"'」』）)】]".indexOf(value) >= 0;
    }

    /** 拼接可能改变BPE边界，按最终正文复核，宁可减少重叠也不截断核心正文。 */
    private String trimToFinalBudget(String prefix, String core, int budget, Integer limit) {
        while (!prefix.isEmpty()) {
            String combined = prefix + SEPARATOR + core;
            if (DocumentTokenCounter.count(prefix) <= budget
                    && (limit == null || DocumentTokenCounter.count(combined) <= limit)) {
                break;
            }
            prefix = prefix.substring(Character.charCount(prefix.codePointAt(0))).stripLeading();
        }
        return prefix;
    }

    private String tailByCodePoints(String text, int count) {
        int start = text.offsetByCodePoints(text.length(), -count);
        return text.substring(start);
    }

    private DocumentChunk prependOverlap(DocumentChunk current, DocumentChunk previous, SourceTail tail, String prefix) {
        String raw = prefix + SEPARATOR + current.rawText();
        String text = prefix + SEPARATOR + (current.text() == null ? current.rawText() : current.text());
        var headings = mergeValues(previous.headings(), current.headings());
        int prefixStart = tail.start() + tail.text().length() - prefix.length();
        List<String> sourceRefs = new ArrayList<>();
        List<Integer> sourcePages = new ArrayList<>();
        for (var span : tail.spans()) {
            if (span.end() <= prefixStart) {
                continue;
            }
            sourceRefs.add(span.block().id());
            for (var location : span.block().locations()) {
                sourcePages.add(location.page());
            }
        }
        var refs = mergeValues(sourceRefs, current.documentItemRefs());
        var pages = mergeValues(sourcePages, current.pageNumbers());
        return new DocumentChunk(current.filename(), current.index(), text, raw, DocumentTokenCounter.count(raw),
                headings, current.captions(), refs, pages, current.metadata());
    }

    private <T> List<T> mergeValues(List<T> first, List<T> second) {
        var values = new LinkedHashSet<>(first);
        values.addAll(second);
        return List.copyOf(values);
    }

    /** 可以定位回前片原始正文的连续普通文本候选。 */
    private record SourceTail(
            /** 保留原始格式的连续尾部。 */
            String text,
            /** 候选在前片原始正文中的UTF-16起点。 */
            int start,
            /** 按原文顺序排列的来源区间，最终只继承实际复制到的来源。 */
            List<SourceSpan> spans) {}

    /** 普通文字来源在前片原始正文中的区间，用于精确筛选引用和页码。 */
    private record SourceSpan(
            /** 已验证的普通文字来源。 */
            DocumentBlock block,
            /** 来源文字终点，不包含。 */
            int end) {}
}
