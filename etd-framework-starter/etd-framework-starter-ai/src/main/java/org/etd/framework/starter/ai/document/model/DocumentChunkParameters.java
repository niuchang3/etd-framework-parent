package org.etd.framework.starter.ai.document.model;

import java.time.Duration;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.Builder;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.constant.DocumentOptionLimits;
import org.etd.framework.starter.ai.document.constant.DocumentImageExportMode;
import org.etd.framework.starter.ai.document.constant.DocumentTableMode;
import org.etd.framework.starter.ai.document.constant.RegexBoundaryPlacement;
import org.etd.framework.starter.ai.document.validation.DocumentOptionValidator;
import org.springframework.util.Assert;

/**
 * 面向分片草稿的统一参数契约，按基础、普通、高级分层，同一能力的选项相邻组织。
 * 不包含向量模型、向量维度或用户确认状态；正则策略在Docling解析后执行二次切分。
 * @param basic 必填基础参数，页码范围属于此层
 * @param normal 必填普通参数对象，未使用的可空字段不配置
 * @param advanced 必填高级参数对象，处理超时必须显式指定
 */
@Builder
public record DocumentChunkParameters(Basic basic, Normal normal, Advanced advanced) {
    /** 校验策略与选项的组合，不静默忽略属于其他策略的参数。 */
    public DocumentChunkParameters {
        Assert.notNull(basic, "基础参数不能为空");
        Assert.notNull(normal, "普通参数不能为空");
        Assert.notNull(advanced, "高级参数不能为空");
        validateOcr(basic, normal);
        validateStrategy(basic, normal, advanced);
    }

    private static void validateOcr(Basic basic, Normal normal) {
        Assert.isTrue(basic.ocr() || !normal.forceOcr()
                        && normal.ocrPreset() == null && normal.ocrLanguages().isEmpty(),
                "OCR关闭时不能配置强制识别、引擎或语言");
    }

    private static void validateStrategy(Basic basic, Normal normal, Advanced advanced) {
        if (basic.strategy() == DocumentChunkStrategy.REGEX) {
            validateRegex(basic, normal);
        } else {
            Assert.isTrue(basic.pattern() == null && normal.boundaryPlacement() == null
                            && normal.mergeSmallChunks() == null && normal.minTokens() == null,
                    "正则切分选项仅适用于正则策略");
        }
        Assert.isTrue(basic.strategy() == DocumentChunkStrategy.HYBRID || basic.mergePeers() == null,
                "相邻同级块自动合并仅适用于混合分片");
        Assert.isTrue(basic.strategy() != DocumentChunkStrategy.HIERARCHICAL
                        || basic.maxTokens() == null && advanced.tokenizer() == null,
                "层级分片不支持Token上限或分词器设置");
    }

    private static void validateRegex(Basic basic, Normal normal) {
        Assert.hasText(basic.pattern(), "正则策略必须指定切分表达式");
        Assert.isTrue(basic.pattern().length() <= DocumentOptionLimits.MAX_REGEX_CHARACTERS, "正则表达式超过长度限制");
        Assert.notNull(normal.boundaryPlacement(), "正则策略必须指定匹配内容保留位置");
        try {
            Pattern.compile(basic.pattern());
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("切分正则表达式语法无效", exception);
        } catch (StackOverflowError error) {
            throw new IllegalArgumentException("正则表达式嵌套过深");
        }
        Assert.isTrue(!Boolean.TRUE.equals(normal.mergeSmallChunks()) || normal.minTokens() != null,
                "合并短片段时必须指定最小Token阈值");
        Assert.isTrue(normal.minTokens() == null || basic.maxTokens() == null
                        || normal.minTokens() <= basic.chunkTokenBudget(),
                "短片段阈值不能超过扣除重叠后的正文Token预算");
    }

    /** 基础参数：处理范围、分片策略、文字识别及表格处理。 */
    @Builder
    public record Basic(
        /** 起始页，从1开始；与结束页同时省略表示处理全部页。 */
        Integer firstPage,
        /** 结束页，包含该页；仅适用PDF和图片。 */
        Integer lastPage,
        /** 分片策略：hierarchical、hybrid或regex。 */
        DocumentChunkStrategy strategy,
        /** 单片Token上限；开启重叠后最终正文按cl100k_base再次校验，核心正文预留overlapTokens预算。 */
        Integer maxTokens,
        /** 最多重叠Token数，默认0关闭；自动生成时回溯前片连续普通文本并优先对齐段落或句首，按cl100k_base严格限制上限。 */
        int overlapTokens,
        /** 仅混合策略：是否合并标题等元数据相同的相邻小块。 */
        Boolean mergePeers,
        /** 仅正则策略：用于识别切分边界的Java正则表达式，不记录正文或规则到日志。 */
        String pattern,
        /** PDF或图片是否启用OCR。 */
        boolean ocr,
        /** 是否识别表格的行列与单元格结构。 */
        boolean tableStructure,
        /** 分片正文中的表格是否使用Markdown形式。 */
        boolean useMarkdownTables) {

        /** 仅校验明确的数值边界，策略关联约束由统一入口校验。 */
        public Basic {
            Assert.notNull(strategy, "分片策略不能为空");
            DocumentOptionValidator.requirePageRange(firstPage, lastPage);
            DocumentOptionValidator.requireTokenCount(maxTokens, "单片Token上限");
            Assert.isTrue(overlapTokens >= 0, "重叠Token数不能为负");
            Assert.isTrue(maxTokens == null || overlapTokens < maxTokens, "重叠Token数必须小于单片Token上限");
            Assert.isTrue(strategy != DocumentChunkStrategy.HYBRID || overlapTokens == 0 || maxTokens != null,
                    "混合分片开启重叠时必须指定单片Token上限");
        }

        /** @return 为重叠预留预算后的正文上限；未设置上限时仍为空，不改变层级策略语义 */
        public Integer chunkTokenBudget() {
            return maxTokens == null ? null : maxTokens - overlapTokens;
        }

        @Override
        public String toString() {
            return "Basic[strategy=" + strategy + ", 正则规则已隐藏]";
        }
    }

    /** 普通参数：正则边界及短片段规则、OCR、表格精度、标题和图片表达。 */
    @Builder
    public record Normal(
        /** 正则匹配内容归入下一片、上一片或丢弃，仅正则策略使用。 */
        RegexBoundaryPlacement boundaryPlacement,
        /** 仅正则策略：是否合并短片段，优先向后，末片向前；不超过Token上限。 */
        Boolean mergeSmallChunks,
        /** 正则短片段的Token阈值，必须大于零。 */
        Integer minTokens,
        /** OCR引擎预设，未指定时由服务选择。 */
        String ocrPreset,
        /** OCR语言列表，空列表使用引擎默认语言。 */
        List<String> ocrLanguages,
        /** 是否强制覆盖已有文本层，必须同时开启OCR。 */
        boolean forceOcr,
        /** 表格识别精度，未指定时由服务决定。 */
        DocumentTableMode tableMode,
        /** 是否推断PDF标题层级，空值沿用服务默认值。 */
        Boolean doPdfHeadingHierarchy,
        /** 是否在分片中序列化Markdown图片引用。 */
        Boolean useMarkdownImages,
        /** 图片占位文本，允许显式空字符串。 */
        String imagePlaceholder) {

        /** 空语言列表表示使用默认语言；固定集合，防止请求后被外部修改。 */
        public Normal {
            ocrLanguages = ocrLanguages == null ? List.of() : List.copyOf(ocrLanguages);
            for (String language : ocrLanguages) {
                Assert.hasText(language, "OCR语言不能为空白");
            }
            DocumentOptionValidator.requireTextWhenPresent(ocrPreset, "OCR预设");
            DocumentOptionValidator.requireTokenCount(minTokens, "短片段Token阈值");
        }
    }

    /** 高级参数：分词器、表格和标题调优、图片输出、Markdown及执行控制。 */
    @Builder
    public record Advanced(
        /** 混合分片使用的分词器；正则策略用于Token计数的约定。 */
        String tokenizer,
        /** 是否匹配PDF文本单元格。 */
        Boolean tableCellMatching,
        /** 标题推断细项，需同时开启标题层级推断。 */
        DocumentHeadingHierarchyOptions pdfHeadingHierarchyOptions,
        /** 是否识别图片类别，服务须具备对应模型。 */
        Boolean doPictureClassification,
        /** 原文结果是否包含图片。 */
        Boolean includeImages,
        /** 原文结果是否包含整页图片。 */
        Boolean includePageImages,
        /** 原文图片导出方式。 */
        DocumentImageExportMode imageExportMode,
        /** 图片缩放倍数；部署实例的上限由服务校验。 */
        Double imagesScale,
        /** 原文Markdown的分页标记，允许显式空字符串。 */
        String mdPageBreakPlaceholder,
        /** 是否使用紧凑Markdown表格。 */
        Boolean mdCompactTables,
        /** 文档解析出错时是否中止。 */
        Boolean abortOnError,
        /** 单份文档处理超时，必填且大于零，不等于HTTP请求超时。 */
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Duration documentTimeout) {

        /** 校验范围，不在框架中固定某台部署实例的容量上限。 */
        public Advanced {
            DocumentOptionValidator.requireTextWhenPresent(tokenizer, "分词器");
            DocumentOptionValidator.requirePositive(imagesScale, "图片缩放倍数");
            Assert.notNull(documentTimeout, "文档处理超时不能为空");
            Assert.isTrue(documentTimeout.compareTo(Duration.ZERO) > 0, "文档处理超时必须大于零");
        }
    }
}
