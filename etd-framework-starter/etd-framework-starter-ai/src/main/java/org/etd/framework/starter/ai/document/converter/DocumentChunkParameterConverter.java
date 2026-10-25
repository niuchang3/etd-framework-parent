package org.etd.framework.starter.ai.document.converter;

import org.etd.framework.starter.ai.document.model.DocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;
import org.etd.framework.starter.ai.document.model.DocumentParseAdvancedOptions;
import org.etd.framework.starter.ai.document.model.DocumentParseOptions;
import org.etd.framework.starter.ai.document.model.HierarchicalDocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.HybridDocumentChunkOptions;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.util.Assert;

/** 将分层参数转换为引擎执行选项，避免业务层逐项拼装对象或重复定义参数含义。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DocumentChunkParameterConverter {
    /** 编译期生成的无状态转换器。 */
    DocumentChunkParameterConverter INSTANCE = Mappers.getMapper(DocumentChunkParameterConverter.class);

    /** @param parameters 已校验的分层参数 @return Docling预处理选项；正则策略也可以使用 */
    @Mapping(target = "ocr", source = "basic.ocr")
    @Mapping(target = "forceOcr", source = "normal.forceOcr")
    @Mapping(target = "ocrLanguages", source = "normal.ocrLanguages")
    @Mapping(target = "tableStructure", source = "basic.tableStructure")
    @Mapping(target = "firstPage", source = "basic.firstPage")
    @Mapping(target = "lastPage", source = "basic.lastPage")
    @Mapping(target = "documentTimeout", source = "advanced.documentTimeout")
    @Mapping(target = "advanced", source = "parameters")
    DocumentParseOptions toParseOptions(DocumentChunkParameters parameters);

    /** @param parameters 已校验参数 @return 解析细项，流水线沿用服务默认值，不开放未部署的模型能力 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "ocrPreset", source = "normal.ocrPreset")
    @Mapping(target = "tableMode", source = "normal.tableMode")
    @Mapping(target = "doPdfHeadingHierarchy", source = "normal.doPdfHeadingHierarchy")
    @Mapping(target = "tableCellMatching", source = "advanced.tableCellMatching")
    @Mapping(target = "pdfHeadingHierarchyOptions", source = "advanced.pdfHeadingHierarchyOptions")
    @Mapping(target = "doPictureClassification", source = "advanced.doPictureClassification")
    @Mapping(target = "includeImages", source = "advanced.includeImages")
    @Mapping(target = "includePageImages", source = "advanced.includePageImages")
    @Mapping(target = "imageExportMode", source = "advanced.imageExportMode")
    @Mapping(target = "imagesScale", source = "advanced.imagesScale")
    @Mapping(target = "mdPageBreakPlaceholder", source = "advanced.mdPageBreakPlaceholder")
    @Mapping(target = "mdCompactTables", source = "advanced.mdCompactTables")
    @Mapping(target = "abortOnError", source = "advanced.abortOnError")
    DocumentParseAdvancedOptions toAdvancedOptions(DocumentChunkParameters parameters);

    /**
     * @param parameters 已校验的分层参数
     * @return Docling原生分片选项，始终保留供编辑使用的原始正文
     * @throws IllegalArgumentException 选择正则策略；该策略须先解析后独立执行二次切分
     */
    default DocumentChunkOptions toNativeChunkOptions(DocumentChunkParameters parameters) {
        Assert.notNull(parameters, "分片参数不能为空");
        return switch (parameters.basic().strategy()) {
            case HIERARCHICAL -> toHierarchicalOptions(parameters);
            case HYBRID -> toHybridOptions(parameters);
            case REGEX -> throw new IllegalArgumentException("正则策略需要先解析文档，再执行二次切分");
        };
    }

    /** @param parameters 已校验参数 @return 层级分片执行选项 */
    @Mapping(target = "useMarkdownTables", source = "basic.useMarkdownTables")
    @Mapping(target = "useMarkdownImages", source = "normal.useMarkdownImages")
    @Mapping(target = "imagePlaceholder", source = "normal.imagePlaceholder")
    @Mapping(target = "includeRawText", constant = "true")
    HierarchicalDocumentChunkOptions toHierarchicalOptions(DocumentChunkParameters parameters);

    /** @param parameters 已校验参数 @return 混合分片执行选项 */
    @Mapping(target = "useMarkdownTables", source = "basic.useMarkdownTables")
    @Mapping(target = "useMarkdownImages", source = "normal.useMarkdownImages")
    @Mapping(target = "imagePlaceholder", source = "normal.imagePlaceholder")
    @Mapping(target = "includeRawText", constant = "true")
    @Mapping(target = "maxTokens", expression = "java(parameters.basic().chunkTokenBudget())")
    @Mapping(target = "mergePeers", source = "basic.mergePeers")
    @Mapping(target = "tokenizer", source = "advanced.tokenizer")
    HybridDocumentChunkOptions toHybridOptions(DocumentChunkParameters parameters);

}
