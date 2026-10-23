package org.etd.framework.starter.ai.document;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.etd.framework.starter.ai.document.constant.DocumentTaskStatus;
import java.util.List;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
import org.etd.framework.starter.ai.document.docling.DoclingDocumentParser;
import org.etd.framework.starter.ai.document.docling.DoclingTaskClient;
import org.etd.framework.starter.ai.document.docling.converter.DoclingResultConverter;
import org.etd.framework.starter.ai.document.model.DocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.DocumentChunkRequest;
import org.etd.framework.starter.ai.document.model.DocumentParseOptions;
import org.etd.framework.starter.ai.document.model.DocumentParseAdvancedOptions;
import org.etd.framework.starter.ai.document.model.DocumentParseRequest;
import org.etd.framework.starter.ai.document.model.HierarchicalDocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.HybridDocumentChunkOptions;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;

/** 显式提供测试服务地址时验证真实引擎；默认测试不下载模型或连接外部服务。 */
@EnabledIfEnvironmentVariable(named = "ETD_TEST_DOCLING_BASE_URL", matches = ".+")
class DoclingChunkLiveTest {
    /** 小型测试文档的混合分块 Token 上限。 */
    private static final int MAX_TOKENS = 64;
    /** 测试原文大小上限，单位字节。 */
    private static final int MAX_FILE_BYTES = 1024 * 1024;

    @ParameterizedTest
    @EnumSource(DocumentChunkStrategy.class)
    void parsesAndChunksRealMarkdown(DocumentChunkStrategy strategy) {
        var parser = createParser();
        var request = createRequest(strategy);
        var result = parser.chunk(request);
        assertThat(result.chunks()).isNotEmpty();
        assertThat(result.documents()).hasSize(1);
        assertThat(result.documents().getFirst().result().status()).isEqualTo(DocumentParseStatus.SUCCESS);
        assertThat(result.chunks().getFirst().headings()).isNotEmpty();
        assertThat(result.chunks().getFirst().rawText()).isNotBlank();
        if (strategy == DocumentChunkStrategy.HYBRID) {
            assertThat(result.chunks()).hasSizeGreaterThan(1);
            assertThat(result.chunks()).allSatisfy(chunk -> assertThat(chunk.tokenCount()).isLessThanOrEqualTo(MAX_TOKENS));
        }
    }

    @ParameterizedTest
    @EnumSource(DocumentChunkStrategy.class)
    void submitsAndFetchesRealChunkTask(DocumentChunkStrategy strategy) throws InterruptedException {
        var parser = createParser();
        var request = createRequest(strategy);
        var task = parser.submitChunk(request);
        Instant deadline = Instant.now().plusSeconds(30);
        while (task.status() != DocumentTaskStatus.COMPLETED && task.status() != DocumentTaskStatus.FAILED
                && Instant.now().isBefore(deadline)) {
            Thread.sleep(500);
            task = parser.fetchStatus(task.taskId());
        }
        assertThat(task.status()).isEqualTo(DocumentTaskStatus.COMPLETED);
        var result = parser.fetchChunkResult(task.taskId());
        assertThat(result.chunks()).isNotEmpty();
        assertThat(result.documents()).hasSize(1);
        assertThat(result.documents().getFirst().result().status()).isEqualTo(DocumentParseStatus.SUCCESS);
    }

    private DocumentChunkRequest createRequest(DocumentChunkStrategy strategy) {
        String body = "# Framework guide\n\n## Document ingestion\n\n"
                + "Document ingestion preserves headings and source context for retrieval. ".repeat(30);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        var resource = new ByteArrayResource(bytes);
        var languages = List.<String>of();
        Duration timeout = Duration.ofSeconds(90);
        var advanced = DocumentParseAdvancedOptions.builder().mdCompactTables(true).mdPageBreakPlaceholder("").build();
        var parseOptions = new DocumentParseOptions(false, false, languages, true, null, null, timeout, advanced);
        var document = new DocumentParseRequest(resource, "chunk-test.md", parseOptions);
        DocumentChunkOptions options = switch (strategy) {
            case HIERARCHICAL -> new HierarchicalDocumentChunkOptions(true, true, true, "[图片]");
            case HYBRID -> new HybridDocumentChunkOptions(true, true, MAX_TOKENS, null, true, true, "[图片]");
        };
        return new DocumentChunkRequest(document, options, true);
    }

    private DoclingDocumentParser createParser() {
        URI baseUrl = URI.create(System.getenv("ETD_TEST_DOCLING_BASE_URL"));
        Duration timeout = Duration.ofSeconds(120);
        Duration connectTimeout = Duration.ofSeconds(5);
        var httpBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER);
        var client = DoclingTaskClient.builder().baseUrl(baseUrl).connectTimeout(connectTimeout)
                .apiKey(System.getenv("ETD_TEST_DOCLING_API_KEY")).readTimeout(timeout)
                .httpClientBuilder(httpBuilder).build();
        var converter = Mappers.getMapper(DoclingResultConverter.class);
        return new DoclingDocumentParser(client, converter, MAX_FILE_BYTES);
    }
}
