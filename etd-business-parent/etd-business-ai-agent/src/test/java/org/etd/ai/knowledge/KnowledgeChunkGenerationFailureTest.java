package org.etd.ai.knowledge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;
import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.etd.ai.knowledge.biz.KnowledgeChunkGenerationBizService;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.exception.KnowledgeChunkGenerationException;
import org.etd.ai.knowledge.exception.KnowledgeChunkGenerationException.Reason;
import org.etd.ai.knowledge.service.KnowledgeChunkGenerationService;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
import org.etd.framework.starter.ai.document.constant.DocumentTaskStatus;
import org.etd.framework.starter.ai.document.constant.RegexBoundaryPlacement;
import org.etd.framework.starter.ai.document.docling.DoclingDocumentParser;
import org.etd.framework.starter.ai.document.docling.DoclingProperties;
import org.etd.framework.starter.ai.document.model.DocumentChunkDocument;
import org.etd.framework.starter.ai.document.model.DocumentChunkParameters;
import org.etd.framework.starter.ai.document.model.DocumentChunkResult;
import org.etd.framework.starter.ai.document.model.DocumentParseResult;
import org.etd.framework.starter.ai.document.model.DocumentParseTask;
import org.etd.framework.starter.ai.document.parser.DocumentParser;
import org.etd.framework.starter.ai.document.parser.DocumentParserRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 贯通引擎失败识别与业务失败响应，避免超时原因再次被泛化提示覆盖。 */
class KnowledgeChunkGenerationFailureTest {
    private DoclingDocumentParser parser;
    private KnowledgeChunkGenerationService generation;
    private ByteArrayResource resource;

    /** 使用真实参数转换和业务服务，只替换外部Docling调用。 */
    @BeforeEach
    void prepare() {
        parser = mock(DoclingDocumentParser.class);
        when(parser.engine()).thenReturn(DoclingDocumentParser.ENGINE);
        List<DocumentParser> parsers = List.of(parser);
        var registry = new DocumentParserRegistry(parsers);
        generation = new KnowledgeChunkGenerationService(registry);
        byte[] content = {1};
        resource = new ByteArrayResource(content);
        var task = new DocumentParseTask(DoclingDocumentParser.ENGINE, "test-task", DocumentTaskStatus.COMPLETED, null);
        when(parser.submit(any())).thenReturn(task);
        when(parser.submitChunk(any())).thenReturn(task);
    }

    /** 所有分片策略均应根据引擎明确诊断报告超时、配置时限与页数，不暴露其他警告内容。 */
    @ParameterizedTest
    @EnumSource(DocumentChunkStrategy.class)
    void reportsTimeoutAndProgressAcrossStrategies(DocumentChunkStrategy strategy) {
        var warnings = List.of("document timeout exceeded",
                "Pipeline stage timeout: processed 117/119 pages successfully, 2 pages failed or incomplete.",
                "sensitive-document-or-provider-message");
        stubResult(DocumentParseStatus.PARTIAL_SUCCESS, warnings);
        var parameters = parameters(strategy);
        assertThatThrownBy(() -> generation.generateChunkList(resource, "sample.pdf", parameters))
                .isInstanceOfSatisfying(KnowledgeChunkGenerationException.class, error -> {
                    assertThat(error.getReason()).isEqualTo(Reason.PARSE_TIMEOUT);
                    assertThat(error.getMessage()).contains("90秒", "117/119页", "2页失败或未完成")
                            .doesNotContain("sensitive-document-or-provider-message");
                });
    }

    /** 引擎没有给出页数时仍可明确报告超时，不能编造处理进度。 */
    @Test
    void reportsTimeoutWithoutInventingProgress() {
        var warnings = List.of("document timeout exceeded");
        stubResult(DocumentParseStatus.FAILURE, warnings);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        assertThatThrownBy(() -> generation.generateChunkList(resource, "sample.pdf", parameters))
                .hasMessageContaining("文档解析超时").hasMessageContaining("90秒").hasMessageNotContaining("已完成");
    }

    /** 部分成功不等于超时，包含正文的未知错误只能进入固定的部分失败提示。 */
    @Test
    void doesNotMisclassifyPartialFailureAsTimeout() {
        var warnings = List.of("sensitive-document-or-provider-message");
        stubResult(DocumentParseStatus.PARTIAL_SUCCESS, warnings);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        assertThatThrownBy(() -> generation.generateChunkList(resource, "sample.pdf", parameters))
                .isInstanceOfSatisfying(KnowledgeChunkGenerationException.class, error -> {
                    assertThat(error.getReason()).isEqualTo(Reason.PARTIAL_PARSE);
                    assertThat(error.getMessage()).contains("仅部分解析成功").doesNotContain("超时", "sensitive");
                });
    }

    /** 返回结果数量异常应独立分类，不能再统一输出文档解析失败。 */
    @Test
    void distinguishesInvalidResultCount() {
        var result = new DocumentChunkResult(List.of(), List.of(), 1.0);
        when(parser.fetchChunkResult("test-task")).thenReturn(result);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        assertThatThrownBy(() -> generation.generateChunkList(resource, "sample.pdf", parameters))
                .isInstanceOfSatisfying(KnowledgeChunkGenerationException.class,
                        error -> assertThat(error.getReason()).isEqualTo(Reason.INVALID_RESULT_COUNT));
    }

    /** 排队及轮询耗尽业务等待预算时，应与引擎报告的文档解析超时区分。 */
    @Test
    void distinguishesTaskWaitTimeout() {
        var task = new DocumentParseTask(DoclingDocumentParser.ENGINE, "test-task", DocumentTaskStatus.RUNNING, null);
        when(parser.submitChunk(any())).thenReturn(task);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var start = Instant.parse("2026-10-25T00:00:00Z");
        var expired = start.plusSeconds(211);
        try (var time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(start, expired);
            assertThatThrownBy(() -> generation.generateChunkList(resource, "sample.pdf", parameters))
                    .isInstanceOfSatisfying(KnowledgeChunkGenerationException.class, error -> {
                        assertThat(error.getReason()).isEqualTo(Reason.WAIT_TIMEOUT);
                        assertThat(error.getMessage()).contains("210秒等待预算").doesNotContain("文档解析超时");
                    });
        }
        verify(parser, never()).fetchChunkResult(any());
    }

    /** 任务整体失败与文档部分成功使用不同分类，不声称已经识别到超时。 */
    @Test
    void distinguishesTaskFailure() {
        var task = new DocumentParseTask(DoclingDocumentParser.ENGINE, "test-task", DocumentTaskStatus.FAILED, null);
        when(parser.submitChunk(any())).thenReturn(task);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        assertThatThrownBy(() -> generation.generateChunkList(resource, "sample.pdf", parameters))
                .isInstanceOfSatisfying(KnowledgeChunkGenerationException.class,
                        error -> assertThat(error.getReason()).isEqualTo(Reason.TASK_FAILED));
    }

    /** 外层业务必须把安全超时提示写入失败状态，保留原有分片与参数，不能再次泛化。 */
    @Test
    void preservesSpecificTimeoutThroughBusinessResponse() throws Exception {
        var warnings = List.of("document timeout exceeded",
                "Pipeline stage timeout: processed 117/119 pages successfully, 2 pages failed or incomplete.");
        stubResult(DocumentParseStatus.PARTIAL_SUCCESS, warnings);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var document = createDocument(parameters);
        var documents = mock(KnowledgeDocumentService.class);
        when(documents.requireDocument(1L, true)).thenReturn(document);
        var objects = mock(KnowledgeObjectService.class);
        when(objects.openObject(document)).thenAnswer(call -> new ByteArrayInputStream(resource.getByteArray()));
        var chunks = mock(KnowledgeChunkService.class);
        var properties = new DoclingProperties();
        var transactions = createTransactions();
        var biz = new KnowledgeChunkGenerationBizService(documents, objects, chunks, generation, properties, transactions);
        var result = biz.generateChunks(1L, 0L, parameters);
        assertThat(result.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.FAILED);
        assertThat(result.getChunkErrorMessage()).contains("文档解析超时", "90秒", "117/119页", "已有分片已保留");
        assertThat(result.getChunkCount()).isEqualTo(2);
        assertThat(result.getChunkParameters()).isEqualTo(parameters);
        verifyNoInteractions(chunks);
    }

    /** 连接被重置和HTTP等待超时应给出具体分类，同时禁止回显引擎名称及底层异常正文。 */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void classifiesServiceTransportFailureWithoutExposingEngine(boolean timeout) throws Exception {
        IOException transport = timeout ? new HttpTimeoutException("Docling endpoint detail")
                : new SocketException("Connection reset");
        var wrapped = new IOException("HTTP/1.1 header parser received no bytes", transport);
        var failure = new DocumentParseException("Docling secret diagnostic", DocumentParseException.NO_HTTP_STATUS, wrapped);
        var result = generateServiceFailure(failure);
        String expected = timeout ? "请求超时" : "连接中断";
        assertThat(result.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.FAILED);
        assertThat(result.getChunkErrorMessage()).contains(expected, "已有分片已保留")
                .doesNotContain("Docling", "secret", "header parser");
        assertThat(result.getChunkCount()).isEqualTo(2);
    }

    /** 文件超限在读取和外部解析前明确返回配置上限，不得泛化为解析服务或内存故障。 */
    @Test
    void reportsConfiguredFileSizeLimitBeforeCallingParser() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var document = createDocument(parameters);
        var properties = new DoclingProperties();
        document.setFileSize(properties.getMaxFileBytes() + 1L);
        var documents = mock(KnowledgeDocumentService.class);
        when(documents.requireDocument(1L, true)).thenReturn(document);
        var objects = mock(KnowledgeObjectService.class);
        var chunks = mock(KnowledgeChunkService.class);
        var transactions = createTransactions();
        var biz = new KnowledgeChunkGenerationBizService(documents, objects, chunks, generation, properties, transactions);
        var result = biz.generateChunks(1L, 0L, parameters);
        assertThat(result.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.FAILED);
        assertThat(result.getChunkErrorMessage()).contains("20 MiB上限", "已有分片已保留")
                .doesNotContain("分片生成失败", "Docling", "超时");
        verifyNoInteractions(objects, chunks);
        verify(parser, never()).submitChunk(any());
    }

    /** 返回边界覆盖列表、详情和分片状态，保留业务诊断数字但不向用户显示内部引擎名称。 */
    @ParameterizedTest
    @ValueSource(strings = {"Docling解析超时，已等待90秒", "DOCLING连接中断，已等待90秒", "docling处理失败，已等待90秒", "请检查Docling服务，已等待90秒"})
    void hidesEngineNameInAllDocumentResponses(String message) {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var document = createDocument(parameters);
        document.setChunkErrorMessage(message);
        var list = KnowledgeDocumentConverter.INSTANCE.toListVO(document);
        var detail = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        var state = KnowledgeChunkConverter.INSTANCE.toStateVO(document);
        var messages = List.of(list.getChunkErrorMessage(), detail.getChunkErrorMessage(), state.getChunkErrorMessage());
        for (String displayed : messages) {
            assertThat(displayed.toLowerCase(Locale.ROOT)).doesNotContain("docling");
            assertThat(displayed).contains("90秒").doesNotContain("服务服务");
        }
        assertThat(document.getChunkErrorMessage()).isEqualTo(message);
    }

    /** 所有固定原因及动态超时提示均必须使用业务术语。 */
    @Test
    void hidesEngineNameInEveryDefinedFailureReason() {
        for (Reason reason : Reason.values()) {
            assertThat(reason.getMessage().toLowerCase(Locale.ROOT)).doesNotContain("docling");
        }
        var timeout = Duration.ofSeconds(90);
        var wait = KnowledgeChunkGenerationException.waitTimeout(timeout);
        assertThat(wait.getMessage().toLowerCase(Locale.ROOT)).doesNotContain("docling");
    }

    /** 复现轮询阶段连接中断，保留真实生成服务和业务失败落库流程。 */
    private KnowledgeDocumentEntity generateServiceFailure(DocumentParseException failure) throws Exception {
        var task = new DocumentParseTask(DoclingDocumentParser.ENGINE, "test-task", DocumentTaskStatus.RUNNING, null);
        when(parser.submitChunk(any())).thenReturn(task);
        when(parser.fetchStatus("test-task")).thenThrow(failure);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var document = createDocument(parameters);
        var documents = mock(KnowledgeDocumentService.class);
        when(documents.requireDocument(1L, true)).thenReturn(document);
        var objects = mock(KnowledgeObjectService.class);
        when(objects.openObject(document)).thenAnswer(call -> new ByteArrayInputStream(resource.getByteArray()));
        var chunks = mock(KnowledgeChunkService.class);
        var properties = new DoclingProperties();
        var transactions = createTransactions();
        var biz = new KnowledgeChunkGenerationBizService(documents, objects, chunks, generation, properties, transactions);
        return biz.generateChunks(1L, 0L, parameters);
    }

    /** 两种接口的任务均已结束，但文档级结果仍可部分成功或失败。 */
    private void stubResult(DocumentParseStatus status, List<String> warnings) {
        var parsed = new DocumentParseResult(status, null, List.of(), null, 91.33, warnings);
        var document = new DocumentChunkDocument("sample.pdf", parsed);
        var documents = List.of(document);
        var chunked = new DocumentChunkResult(List.of(), documents, 91.33);
        when(parser.fetchResult("test-task")).thenReturn(parsed);
        when(parser.fetchChunkResult("test-task")).thenReturn(chunked);
    }

    /** 保留原有成功参数和数量，让失败路径的保留行为可以被直接验证。 */
    private KnowledgeDocumentEntity createDocument(DocumentChunkParameters parameters) {
        var document = new KnowledgeDocumentEntity();
        document.setId(1L);
        document.setVersion(0L);
        document.setChunkStatus(KnowledgeChunkStatus.DRAFT);
        document.setChunkCount(2);
        document.setChunkParameters(parameters);
        document.setUploadStatus(KnowledgePolicy.COMPLETED);
        document.setOriginalFilename("sample.pdf");
        document.setFileSize(1L);
        return document;
    }

    /** 本测试只覆盖业务消息贯通，不模拟数据库回滚；事务语义由既有持久化测试覆盖。 */
    private TransactionTemplate createTransactions() {
        var transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(call -> {
            TransactionCallback<?> callback = call.getArgument(0);
            var status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        });
        return transactions;
    }

    /** 参数与本次用户失败请求一致，正则策略额外提供必需表达式。 */
    private DocumentChunkParameters parameters(DocumentChunkStrategy strategy) {
        var basic = DocumentChunkParameters.Basic.builder().strategy(strategy);
        var normalOptions = DocumentChunkParameters.Normal.builder();
        if (strategy != DocumentChunkStrategy.HIERARCHICAL) {
            basic.maxTokens(512);
        }
        if (strategy == DocumentChunkStrategy.REGEX) {
            basic.pattern("(?m)^# ");
            normalOptions.boundaryPlacement(RegexBoundaryPlacement.NEXT);
        }
        var basicOptions = basic.build();
        var normal = normalOptions.build();
        var timeout = Duration.ofSeconds(90);
        var advanced = DocumentChunkParameters.Advanced.builder().documentTimeout(timeout).build();
        return new DocumentChunkParameters(basicOptions, normal, advanced);
    }
}
