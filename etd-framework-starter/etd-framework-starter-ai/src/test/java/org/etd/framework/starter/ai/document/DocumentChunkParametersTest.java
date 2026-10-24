package org.etd.framework.starter.ai.document;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.constant.DocumentTableMode;
import org.etd.framework.starter.ai.document.constant.RegexBoundaryPlacement;
import org.etd.framework.starter.ai.document.converter.DocumentChunkParameterConverter;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;
import org.etd.framework.starter.ai.document.model.DocumentHeadingHierarchyOptions;
import org.etd.framework.starter.ai.document.model.HybridDocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.HierarchicalDocumentChunkOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 验证分层参数的能力归并、策略约束及实际MapStruct转换，不调用远端服务。 */
class DocumentChunkParametersTest {
    private final DocumentChunkParameterConverter converter = DocumentChunkParameterConverter.INSTANCE;

    /** 基础、普通、高级各层的同一能力参数在引擎选项中正确汇合。 */
    @Test
    void mapsHybridParametersAndPreservesRawTextForEditing() {
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HYBRID)
                .firstPage(2).lastPage(4).maxTokens(512).mergePeers(false)
                .ocr(true).tableStructure(true).useMarkdownTables(true).build();
        var languages = List.of("en");
        var normal = DocumentChunkParameters.Normal.builder().ocrPreset("easyocr").ocrLanguages(languages)
                .forceOcr(true).tableMode(DocumentTableMode.FAST).doPdfHeadingHierarchy(true)
                .useMarkdownImages(true).imagePlaceholder("").build();
        var headings = DocumentHeadingHierarchyOptions.builder().maxLevel(4).build();
        var advanced = DocumentChunkParameters.Advanced.builder().tokenizer("test-tokenizer")
                .pdfHeadingHierarchyOptions(headings).documentTimeout(Duration.ofSeconds(90)).build();
        var parameters = new DocumentChunkParameters(basic, normal, advanced);
        var parsing = converter.toParseOptions(parameters);
        assertThat(parsing.firstPage()).isEqualTo(2);
        assertThat(parsing.lastPage()).isEqualTo(4);
        assertThat(parsing.ocr()).isTrue();
        assertThat(parsing.forceOcr()).isTrue();
        assertThat(parsing.advanced().ocrPreset()).isEqualTo("easyocr");
        assertThat(parsing.advanced().pdfHeadingHierarchyOptions().maxLevel()).isEqualTo(4);
        var chunking = (HybridDocumentChunkOptions) converter.toNativeChunkOptions(parameters);
        assertThat(chunking.maxTokens()).isEqualTo(512);
        assertThat(chunking.mergePeers()).isFalse();
        assertThat(chunking.tokenizer()).isEqualTo("test-tokenizer");
        assertThat(chunking.imagePlaceholder()).isEmpty();
        assertThat(chunking.includeRawText()).isTrue();
    }

    /** 正则选项可用于预处理，不能错误地提交给Docling的原生分片接口。 */
    @Test
    void keepsRegexContractSeparateFromNativeExecution() throws Exception {
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.REGEX)
                .pattern("(?m)^#{1,3}\\s+").maxTokens(512).build();
        var normal = DocumentChunkParameters.Normal.builder().boundaryPlacement(RegexBoundaryPlacement.NEXT)
                .mergeSmallChunks(true).minTokens(128).build();
        var parameters = parameters(basic, normal);
        var parsing = converter.toParseOptions(parameters);
        assertThat(parsing.ocr()).isFalse();
        assertThat(parsing.ocrLanguages()).isEmpty();
        assertThat(parsing.advanced().pipeline()).isNull();
        assertThatThrownBy(() -> converter.toNativeChunkOptions(parameters))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("正则策略");
        var json = JsonMapper.builder().findAndAddModules().build();
        var payload = json.valueToTree(parameters);
        assertThat(payload.path("basic").path("strategy").asText()).isEqualTo("regex");
        assertThat(payload.path("normal").path("boundaryPlacement").asText()).isEqualTo("next");
        assertThat(parameters.toString()).doesNotContain(basic.pattern());
    }

    /** 层级策略保留原生语义，不引入隐式Token上限或默认分词器。 */
    @Test
    void mapsHierarchicalWithoutTokenSettings() {
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HIERARCHICAL)
                .useMarkdownTables(true).build();
        var normal = DocumentChunkParameters.Normal.builder().build();
        var parameters = parameters(basic, normal);
        var chunking = (HierarchicalDocumentChunkOptions) converter.toNativeChunkOptions(parameters);
        assertThat(chunking.useMarkdownTables()).isTrue();
        assertThat(chunking.includeRawText()).isTrue();
        assertThat(converter.toParseOptions(parameters).advanced().pipeline()).isNull();
    }

    /** 非正则策略不能夹带正则参数，层级策略不能夹带混合切分参数。 */
    @Test
    void rejectsOptionsBelongingToAnotherStrategy() {
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HYBRID)
                .pattern("chapter").build();
        var normal = DocumentChunkParameters.Normal.builder().build();
        assertThatThrownBy(() -> parameters(basic, normal)).isInstanceOf(IllegalArgumentException.class);
        var hierarchical = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HIERARCHICAL)
                .maxTokens(512).build();
        assertThatThrownBy(() -> parameters(hierarchical, normal)).isInstanceOf(IllegalArgumentException.class);
    }

    /** 正则表达式、边界位置和短片段阈值必须形成可解释的参数组合。 */
    @Test
    void rejectsInvalidRegexAndChunkThresholds() {
        var invalid = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.REGEX)
                .pattern("(").build();
        var normal = DocumentChunkParameters.Normal.builder().boundaryPlacement(RegexBoundaryPlacement.NEXT).build();
        assertThatThrownBy(() -> parameters(invalid, normal)).isInstanceOf(IllegalArgumentException.class);
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.REGEX)
                .pattern("chapter").maxTokens(128).build();
        var missing = DocumentChunkParameters.Normal.builder().boundaryPlacement(RegexBoundaryPlacement.NEXT)
                .mergeSmallChunks(true).build();
        assertThatThrownBy(() -> parameters(basic, missing)).isInstanceOf(IllegalArgumentException.class);
        var oversized = DocumentChunkParameters.Normal.builder().boundaryPlacement(RegexBoundaryPlacement.NEXT)
                .mergeSmallChunks(true).minTokens(129).build();
        assertThatThrownBy(() -> parameters(basic, oversized)).isInstanceOf(IllegalArgumentException.class);
    }

    /** 语言列表与页码范围在请求入口固定，不受外部集合修改或缺失页码影响。 */
    @Test
    void validatesPageRangesAndFreezesOcrLanguages() {
        assertThatThrownBy(() -> DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HYBRID)
                .firstPage(1).build()).isInstanceOf(IllegalArgumentException.class);
        var languages = new ArrayList<String>();
        languages.add("en");
        var normal = DocumentChunkParameters.Normal.builder().ocrLanguages(languages).build();
        languages.clear();
        assertThat(normal.ocrLanguages()).containsExactly("en");
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HYBRID).build();
        assertThatThrownBy(() -> parameters(basic, normal)).isInstanceOf(IllegalArgumentException.class);
    }

    /** 重叠属于基础参数，传给原生混合分片器的预算须扣除重叠，JSON仍保留用户原始上限。 */
    @Test
    void reservesOverlapBudgetAndSerializesBasicOption() throws Exception {
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HYBRID)
                .maxTokens(512).overlapTokens(50).build();
        var normal = DocumentChunkParameters.Normal.builder().build();
        var parameters = parameters(basic, normal);
        var nativeOptions = (HybridDocumentChunkOptions) converter.toNativeChunkOptions(parameters);
        assertThat(nativeOptions.maxTokens()).isEqualTo(462);
        var json = JsonMapper.builder().findAndAddModules().build();
        var payload = json.valueToTree(parameters);
        assertThat(payload.path("basic").path("maxTokens").asInt()).isEqualTo(512);
        assertThat(payload.path("basic").path("overlapTokens").asInt()).isEqualTo(50);
        var restored = json.treeToValue(payload, DocumentChunkParameters.class);
        assertThat(restored).isEqualTo(parameters);
    }

    /** 重叠不可为负或占满正文预算；混合策略须显式提供上限，短片段阈值也必须适配预留预算。 */
    @Test
    void rejectsInvalidOverlapBudget() {
        assertThatThrownBy(() -> DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.REGEX)
                .overlapTokens(-1).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HYBRID)
                .maxTokens(64).overlapTokens(64).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.HYBRID)
                .overlapTokens(10).build()).isInstanceOf(IllegalArgumentException.class);
        var basic = DocumentChunkParameters.Basic.builder().strategy(DocumentChunkStrategy.REGEX)
                .pattern("rule").maxTokens(64).overlapTokens(16).build();
        var normal = DocumentChunkParameters.Normal.builder().boundaryPlacement(RegexBoundaryPlacement.NEXT)
                .mergeSmallChunks(true).minTokens(49).build();
        assertThatThrownBy(() -> parameters(basic, normal)).isInstanceOf(IllegalArgumentException.class);
    }

    private DocumentChunkParameters parameters(DocumentChunkParameters.Basic basic, DocumentChunkParameters.Normal normal) {
        var timeout = Duration.ofSeconds(90);
        var advanced = DocumentChunkParameters.Advanced.builder().documentTimeout(timeout).build();
        return new DocumentChunkParameters(basic, normal, advanced);
    }
}
