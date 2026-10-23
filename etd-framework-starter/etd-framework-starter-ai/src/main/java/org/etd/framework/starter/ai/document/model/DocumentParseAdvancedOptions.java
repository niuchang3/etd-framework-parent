package org.etd.framework.starter.ai.document.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;
import lombok.Builder;
import org.etd.framework.starter.ai.document.constant.DocumentImageExportMode;
import org.etd.framework.starter.ai.document.constant.DocumentPdfBackend;
import org.etd.framework.starter.ai.document.constant.DocumentProcessingPipeline;
import org.etd.framework.starter.ai.document.constant.DocumentTableMode;
import org.springframework.util.Assert;

/**
 * Docling原生解析的高级选项；空值不下发，由服务决定默认值。
 * 自定义配置使用官方引擎schema，不提供任意顶层请求字段透传，不在日志中输出配置。
 * 此处只影响解析和分片草稿，不执行向量化或业务确认。
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DocumentParseAdvancedOptions(
        /** OCR引擎预设ID；不填由服务选择，使用ocrPreset而非已废弃ocrEngine。 */
        String ocrPreset,
        /** OCR引擎专属配置，字段须符合所选引擎schema；服务须允许自定义配置。 */
        Map<String, Object> ocrCustomConfig,
        /** PDF原生解析后端；不填使用服务默认后端。 */
        DocumentPdfBackend pdfBackend,
        /** PDF或图片流水线；不填使用服务默认值，VLM须预先配置模型。 */
        DocumentProcessingPipeline pipeline,
        /** 表格结构识别精度：fast或accurate。 */
        DocumentTableMode tableMode,
        /** 是否将模型预测的表格单元格与PDF文本单元格匹配。 */
        Boolean tableCellMatching,
        /** 解析出错时是否中止本次文档处理。 */
        Boolean abortOnError,
        /** 是否从书签、编号和样式推断PDF及图片标题层级，仅标准流水线生效。 */
        Boolean doPdfHeadingHierarchy,
        /** 标题层级推断细项，必须同时显式开启doPdfHeadingHierarchy。 */
        DocumentHeadingHierarchyOptions pdfHeadingHierarchyOptions,
        /** 是否生成并在原文结果中包含图片；不填沿用适配器不导出图片的设置。 */
        Boolean includeImages,
        /** 是否在原文结果中附带整页图片。 */
        Boolean includePageImages,
        /** 原文图片导出方式；不填沿用占位符方式。 */
        DocumentImageExportMode imageExportMode,
        /** 生成图片的缩放倍数，必须为有限正数。 */
        Double imagesScale,
        /** 原文Markdown分页占位文本；允许显式空字符串。 */
        String mdPageBreakPlaceholder,
        /** 原文Markdown表格是否省略对齐填充空格。 */
        Boolean mdCompactTables,
        /** 是否增强代码识别；可能加载额外模型。 */
        Boolean doCodeEnrichment,
        /** 是否增强公式识别并生成LaTeX；可能加载额外模型。 */
        Boolean doFormulaEnrichment,
        /** 是否对图片分类；可能加载额外模型。 */
        Boolean doPictureClassification,
        /** 是否生成图片描述；服务须具备对应模型。 */
        Boolean doPictureDescription,
        /** 图片描述的最小页面面积占比，范围0至1。 */
        Double pictureDescriptionAreaThreshold,
        /** 是否从图表提取数值；服务须具备对应视觉模型。 */
        Boolean doChartExtraction,
        /** 视觉解析流水线的模型预设ID，由服务管理。 */
        String vlmPipelinePreset,
        /** 图片描述的模型预设ID，由服务管理。 */
        String pictureDescriptionPreset,
        /** 图片分类的模型预设ID，由服务管理。 */
        String pictureClassificationPreset,
        /** 图表提取的模型预设ID，由服务管理。 */
        String chartExtractionPreset,
        /** 代码和公式增强的模型预设ID，由服务管理。 */
        String codeFormulaPreset,
        /** 表格结构模型预设ID，由服务管理。 */
        String tableStructurePreset,
        /** 版面识别模型预设ID，由服务管理。 */
        String layoutPreset,
        /** 视觉解析模型及执行器的官方自定义配置，须由服务允许。 */
        Map<String, Object> vlmPipelineCustomConfig,
        /** 图片描述模型及执行器的官方自定义配置，须由服务允许。 */
        Map<String, Object> pictureDescriptionCustomConfig,
        /** 图片分类模型的官方自定义配置，须由服务允许。 */
        Map<String, Object> pictureClassificationCustomConfig,
        /** 图表提取模型及执行器的官方自定义配置，须由服务允许。 */
        Map<String, Object> chartExtractionCustomConfig,
        /** 代码和公式模型及执行器的官方自定义配置，须由服务允许。 */
        Map<String, Object> codeFormulaCustomConfig,
        /** 表格结构模型的官方自定义配置，须由服务允许。 */
        Map<String, Object> tableStructureCustomConfig,
        /** 版面识别模型的官方自定义配置，须由服务允许。 */
        Map<String, Object> layoutCustomConfig) {

    /** 验证可在本地确定的范围；引擎专属配置交由Docling按官方schema验证。 */
    public DocumentParseAdvancedOptions {
        requirePositive(imagesScale, "图片缩放倍数");
        requireRatio(pictureDescriptionAreaThreshold, "图片描述面积阈值");
        Assert.isTrue(pdfHeadingHierarchyOptions == null || Boolean.TRUE.equals(doPdfHeadingHierarchy),
                "标题层级细项必须同时开启标题层级推断");
        Assert.isTrue(!Boolean.TRUE.equals(doPdfHeadingHierarchy)
                        || pipeline == null || pipeline == DocumentProcessingPipeline.STANDARD,
                "标题层级推断仅支持标准解析流水线");
        requirePreset(ocrPreset);
        ocrCustomConfig = ocrCustomConfig == null ? null : Map.copyOf(ocrCustomConfig);
        requirePreset(vlmPipelinePreset);
        requirePreset(pictureDescriptionPreset);
        requirePreset(pictureClassificationPreset);
        requirePreset(chartExtractionPreset);
        requirePreset(codeFormulaPreset);
        requirePreset(tableStructurePreset);
        requirePreset(layoutPreset);
        vlmPipelineCustomConfig = vlmPipelineCustomConfig == null ? null : Map.copyOf(vlmPipelineCustomConfig);
        pictureDescriptionCustomConfig = pictureDescriptionCustomConfig == null ? null : Map.copyOf(pictureDescriptionCustomConfig);
        pictureClassificationCustomConfig = pictureClassificationCustomConfig == null ? null : Map.copyOf(pictureClassificationCustomConfig);
        chartExtractionCustomConfig = chartExtractionCustomConfig == null ? null : Map.copyOf(chartExtractionCustomConfig);
        codeFormulaCustomConfig = codeFormulaCustomConfig == null ? null : Map.copyOf(codeFormulaCustomConfig);
        tableStructureCustomConfig = tableStructureCustomConfig == null ? null : Map.copyOf(tableStructureCustomConfig);
        layoutCustomConfig = layoutCustomConfig == null ? null : Map.copyOf(layoutCustomConfig);
    }

    /** @return 是否显式配置了仅对PDF或图片流水线有效的选项 */
    public boolean requiresPdfPipeline() {
        return ocrPreset != null || ocrCustomConfig != null || pdfBackend != null || pipeline != null
                || tableMode != null || tableCellMatching != null || doPdfHeadingHierarchy != null
                || pdfHeadingHierarchyOptions != null || doCodeEnrichment != null || doFormulaEnrichment != null
                || doPictureClassification != null || doPictureDescription != null || doChartExtraction != null
                || pictureDescriptionAreaThreshold != null || vlmPipelinePreset != null
                || pictureDescriptionPreset != null || pictureClassificationPreset != null
                || chartExtractionPreset != null || codeFormulaPreset != null || tableStructurePreset != null
                || layoutPreset != null || vlmPipelineCustomConfig != null || pictureDescriptionCustomConfig != null
                || pictureClassificationCustomConfig != null || chartExtractionCustomConfig != null
                || codeFormulaCustomConfig != null || tableStructureCustomConfig != null || layoutCustomConfig != null;
    }

    private static void requirePreset(String value) {
        if (value != null) {
            Assert.hasText(value, "预设ID不能为空白");
        }
    }

    private static void requirePositive(Double value, String name) {
        Assert.isTrue(value == null || Double.isFinite(value) && value > 0, name + "必须为有限正数");
    }

    private static void requireRatio(Double value, String name) {
        Assert.isTrue(value == null || Double.isFinite(value) && value >= 0 && value <= 1,
                name + "必须在0至1之间");
    }

    @Override
    public String toString() {
        return "DocumentParseAdvancedOptions[引擎配置已隐藏]";
    }
}
