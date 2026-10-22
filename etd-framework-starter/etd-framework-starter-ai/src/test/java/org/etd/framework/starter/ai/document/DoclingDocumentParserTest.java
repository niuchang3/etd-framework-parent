package org.etd.framework.starter.ai.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.etd.framework.starter.ai.document.constant.DocumentBlockType;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
import org.etd.framework.starter.ai.document.constant.DocumentTaskStatus;
import org.etd.framework.starter.ai.document.docling.DoclingDocumentParser;
import org.etd.framework.starter.ai.document.docling.DoclingTaskClient;
import org.etd.framework.starter.ai.document.docling.converter.DoclingResultConverter;
import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.framework.starter.ai.document.model.DocumentParseOptions;
import org.etd.framework.starter.ai.document.model.DocumentParseRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.model.DocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.DocumentChunkRequest;
import org.etd.framework.starter.ai.document.model.HierarchicalDocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.HybridDocumentChunkOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 通过本地 HTTP 服务验证官方 SDK 的实际传输和结构转换，不依赖真实 Docling 模型。 */
class DoclingDocumentParserTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<String> paths = new ArrayList<>();
    private HttpServer server;
    private DoclingDocumentParser parser;
    private JsonNode requestBody;
    private String apiKey;
    private int responseStatus = 200;
    private String responseBody = "";
    private long responseDelayMillis;
    private String redirect;

    @BeforeEach
    void startServer() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 0);
        server = HttpServer.create(address, 0);
        server.createContext("/", this::respond);
        server.start();
        parser = createParser(1024);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void submitReturnsImmediatelyWithoutBackgroundPolling() throws Exception {
        responseBody = """
                {"task_id":"task-1","task_status":"pending","task_position":2}
                """;
        var request = request("sample.pdf");
        var task = parser.submit(request);
        assertThat(task.engine()).isEqualTo(DoclingDocumentParser.ENGINE);
        assertThat(task.status()).isEqualTo(DocumentTaskStatus.PENDING);
        assertThat(task.queuePosition()).isEqualTo(2);
        assertThat(paths).containsExactly("/v1/convert/source/async");
        assertThat(apiKey).isEqualTo("test-only-key");
        assertThat(requestBody.path("sources").path(0).path("filename").asText()).isEqualTo("sample.pdf");
        String encoded = requestBody.path("sources").path(0).path("base64_string").asText();
        assertThat(Base64.getDecoder().decode(encoded)).isEqualTo("test document".getBytes(StandardCharsets.UTF_8));
        assertThat(requestBody.path("options").path("to_formats").toString()).isEqualTo("[\"md\",\"json\"]");
        assertThat(requestBody.path("options").path("page_range").toString()).isEqualTo("[1,2]");
        assertThat(requestBody.path("options").path("document_timeout").asDouble()).isEqualTo(120);
    }

    @Test
    void fetchStatusIsOneRequestAndPreservesFailure() {
        responseBody = "{\"task_id\":\"task-1\",\"task_status\":\"failure\"}";
        var task = parser.fetchStatus("task-1");
        assertThat(task.status()).isEqualTo(DocumentTaskStatus.FAILED);
        assertThat(paths).containsExactly("/v1/status/poll/task-1?wait=0");
    }

    @Test
    void resultPreservesPartialSuccessReadingOrderTablesAndCoordinates() {
        responseBody = resultResponse("partial_success");
        var result = parser.fetchResult("task-1");
        assertThat(result.status()).isEqualTo(DocumentParseStatus.PARTIAL_SUCCESS);
        assertThat(result.markdown()).isEqualTo("# 标题");
        assertThat(result.warnings()).containsExactly("部分页面无法识别");
        assertThat(result.blocks()).extracting(block -> block.id())
                .containsExactly("#/texts/1", "#/tables/0", "#/texts/0");
        var heading = result.blocks().getFirst();
        assertThat(heading.type()).isEqualTo(DocumentBlockType.HEADING);
        assertThat(heading.headingLevel()).isEqualTo(2);
        var position = heading.locations().getFirst();
        assertThat(position.page()).isEqualTo(1);
        assertThat(position.top()).isEqualTo(20);
        assertThat(position.bottom()).isEqualTo(40);
        assertThat(position.pageHeight()).isEqualTo(800);
        assertThat(result.blocks().get(1).tableData().rowCount()).isEqualTo(1);
        var cell = result.blocks().get(1).tableData().cells().getFirst();
        assertThat(cell.text()).isEqualTo("单元格");
        assertThat(cell.firstColumn()).isEqualTo(0);
        assertThat(cell.endColumn()).isEqualTo(2);
        assertThat(cell.columnHeader()).isTrue();
        assertThat(result.toString()).doesNotContain("标题", "无法识别");
        assertThat(paths).containsExactly("/v1/result/task-1");
    }

    @Test
    void failureWithoutDocumentRemainsAnExplicitResult() {
        responseBody = "{\"status\":\"failure\",\"errors\":[]}";
        var result = parser.fetchResult("task-1");
        assertThat(result.status()).isEqualTo(DocumentParseStatus.FAILURE);
        assertThat(result.blocks()).isEmpty();
    }

    @Test
    void rejectsMalformedAndUnexpectedResults() {
        responseBody = resultResponse("unexpected");
        assertThatThrownBy(() -> parser.fetchResult("task-1")).isInstanceOf(DocumentParseException.class);
        responseBody = "{\"status\":\"success\"}";
        assertThatThrownBy(() -> parser.fetchResult("task-1")).isInstanceOf(DocumentParseException.class);
        responseBody = "not-json";
        assertThatThrownBy(() -> parser.fetchResult("task-1")).isInstanceOf(DocumentParseException.class);
    }

    @Test
    void rejectsCyclicReferencesRatherThanLooping() {
        responseBody = resultResponse("success").replace("{\"$ref\":\"#/texts/1\"}", "{\"$ref\":\"#/body\"}");
        assertThatThrownBy(() -> parser.fetchResult("task-1")).isInstanceOf(DocumentParseException.class);
    }

    @Test
    void errorsExposeStatusWithoutResponseBodyAndDoNotRetry() {
        responseStatus = 503;
        responseBody = "sensitive-upstream-content";
        assertThatThrownBy(() -> parser.fetchStatus("task-1"))
                .isInstanceOfSatisfying(DocumentParseException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(503);
                    assertThat(exception.getMessage()).doesNotContain("sensitive-upstream-content");
                });
        assertThat(paths).hasSize(1);
    }

    @Test
    void validatesFileSizeFormatAndTaskIdBeforeNetworkCall() {
        var largeRequest = request("sample.pdf");
        var smallParser = createParser(2);
        assertThatThrownBy(() -> smallParser.submit(largeRequest)).isInstanceOf(IllegalArgumentException.class);
        var unsupportedRequest = request("sample.exe");
        assertThatThrownBy(() -> parser.submit(unsupportedRequest)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.fetchStatus("../other")).isInstanceOf(IllegalArgumentException.class);
        assertThat(paths).isEmpty();
    }

    @Test
    void requestTimeoutIsReportedWithoutAutomaticRetry() {
        responseDelayMillis = 300;
        responseBody = "{\"task_id\":\"task-1\",\"task_status\":\"pending\"}";
        Duration timeout = Duration.ofMillis(50);
        var shortTimeoutParser = createParser(1024, timeout);
        assertThatThrownBy(() -> shortTimeoutParser.fetchStatus("task-1"))
                .isInstanceOf(DocumentParseException.class);
    }

    @Test
    void redirectDoesNotForwardCredentialsOrSubmitAgain() {
        responseStatus = 307;
        redirect = "/unexpected-target";
        responseBody = "redirect";
        var request = request("sample.pdf");
        assertThatThrownBy(() -> parser.submit(request)).isInstanceOf(DocumentParseException.class);
        assertThat(paths).containsExactly("/v1/convert/source/async");
    }

    @Test
    void rejectsTaskIdentityMismatchAndUnknownStatus() {
        responseBody = "{\"task_id\":\"other-task\",\"task_status\":\"pending\"}";
        assertThatThrownBy(() -> parser.fetchStatus("task-1")).isInstanceOf(DocumentParseException.class);
        responseBody = "{\"task_id\":\"task-1\",\"task_status\":\"unknown\"}";
        assertThatThrownBy(() -> parser.fetchStatus("task-1")).isInstanceOf(DocumentParseException.class);
    }

    @ParameterizedTest
    @EnumSource(DocumentChunkStrategy.class)
    void synchronousChunkingPreservesOptionsAndContext(DocumentChunkStrategy strategy) throws Exception {
        responseBody = chunkResponse(true);
        DocumentChunkRequest request = chunkRequest(strategy, true);
        var result = parser.chunk(request);
        String expectedPath = "/v1/chunk/" + strategy.getCode() + "/source";
        assertThat(paths).containsExactly(expectedPath);
        assertThat(requestBody.path("include_converted_doc").asBoolean()).isTrue();
        assertThat(requestBody.path("convert_options").path("do_ocr").asBoolean()).isTrue();
        var options = requestBody.path("chunking_options");
        assertThat(options.path("chunker").asText()).isEqualTo(strategy.getCode());
        assertThat(options.path("use_markdown_tables").asBoolean()).isTrue();
        assertThat(options.path("include_raw_text").asBoolean()).isTrue();
        if (strategy == DocumentChunkStrategy.HYBRID) {
            assertThat(options.path("max_tokens").asInt()).isEqualTo(128);
            assertThat(options.path("tokenizer").asText()).isEqualTo("test-tokenizer");
            assertThat(options.path("merge_peers").asBoolean()).isFalse();
        }
        var chunk = result.chunks().getFirst();
        assertThat(chunk.index()).isZero();
        assertThat(chunk.text()).isEqualTo("标题\n正文");
        assertThat(chunk.rawText()).isEqualTo("正文");
        assertThat(chunk.tokenCount()).isEqualTo(12);
        assertThat(chunk.headings()).containsExactly("标题");
        assertThat(chunk.captions()).containsExactly("表格说明");
        assertThat(chunk.documentItemRefs()).containsExactly("#/texts/0");
        assertThat(chunk.pageNumbers()).containsExactly(1);
        assertThat(chunk.metadata()).containsEntry("custom", "retained");
        var original = result.documents().getFirst().result();
        assertThat(original.status()).isEqualTo(DocumentParseStatus.PARTIAL_SUCCESS);
        assertThat(original.blocks()).hasSize(3);
        assertThat(original.warnings()).containsExactly("部分页面无法识别");
        assertThat(chunk.toString()).doesNotContain("正文");
    }

    @ParameterizedTest
    @EnumSource(DocumentChunkStrategy.class)
    void asynchronousChunkSubmissionDoesNotPollAndUsesChunkResult(DocumentChunkStrategy strategy) throws Exception {
        responseBody = "{\"task_id\":\"chunk-1\",\"task_type\":\"chunk\",\"task_status\":\"pending\"}";
        var request = chunkRequest(strategy, false);
        var task = parser.submitChunk(request);
        assertThat(task.status()).isEqualTo(DocumentTaskStatus.PENDING);
        String submitPath = "/v1/chunk/" + strategy.getCode() + "/source/async";
        assertThat(paths).containsExactly(submitPath);
        responseBody = "{\"task_id\":\"chunk-1\",\"task_type\":\"chunk\",\"task_status\":\"success\"}";
        assertThat(parser.fetchStatus("chunk-1").status()).isEqualTo(DocumentTaskStatus.COMPLETED);
        responseBody = chunkResponse(false);
        var result = parser.fetchChunkResult("chunk-1");
        assertThat(result.chunks()).hasSize(1);
        assertThat(result.documents()).isEmpty();
        assertThat(paths).containsExactly(submitPath, "/v1/status/poll/chunk-1?wait=0", "/v1/result/chunk-1");
    }

    @Test
    void preservesServiceDefaultsForOptionalHybridSettings() throws Exception {
        responseBody = chunkResponse(false);
        var options = new HybridDocumentChunkOptions(false, false, null, null, null);
        var document = request("sample.pdf");
        var request = new DocumentChunkRequest(document, options, false);
        parser.chunk(request);
        var payload = requestBody.path("chunking_options");
        assertThat(payload.has("max_tokens")).isFalse();
        assertThat(payload.has("tokenizer")).isFalse();
        assertThat(payload.has("merge_peers")).isFalse();
        assertThat(parser.capabilities().chunkStrategies()).containsExactlyInAnyOrder(
                DocumentChunkStrategy.HIERARCHICAL, DocumentChunkStrategy.HYBRID);
    }

    @Test
    void rejectsWrongTaskResultShapeAndUnknownDocumentStatus() throws Exception {
        responseBody = resultResponse("success");
        assertThatThrownBy(() -> parser.fetchChunkResult("task-1")).isInstanceOf(DocumentParseException.class);
        responseBody = "{\"chunks\":null,\"documents\":[]}";
        assertThatThrownBy(() -> parser.fetchChunkResult("task-1")).isInstanceOf(DocumentParseException.class);
        responseBody = "{\"chunks\":[null],\"documents\":[]}";
        assertThatThrownBy(() -> parser.fetchChunkResult("task-1")).isInstanceOf(DocumentParseException.class);
        responseBody = chunkResponse(true).replace("partial_success", "unknown");
        assertThatThrownBy(() -> parser.fetchChunkResult("task-1")).isInstanceOf(DocumentParseException.class);
    }

    @Test
    void failedDocumentIsPreservedEvenWhenThereAreNoChunks() {
        responseBody = """
                {"chunks":[],"documents":[{"status":"failure","errors":[{"error_message":"failed"}]}],
                 "processing_time":1}
                """;
        var result = parser.fetchChunkResult("task-1");
        assertThat(result.chunks()).isEmpty();
        assertThat(result.documents().getFirst().result().status()).isEqualTo(DocumentParseStatus.FAILURE);
        assertThat(result.documents().getFirst().result().warnings()).containsExactly("failed");
    }

    @Test
    void invalidHybridSettingsFailBeforeCallingService() {
        assertThatThrownBy(() -> new HybridDocumentChunkOptions(false, false, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HybridDocumentChunkOptions(false, false, 128, " ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(paths).isEmpty();
    }

    @Test
    void chunkSubmissionDoesNotRetryRemoteFailure() {
        responseStatus = 503;
        responseBody = "upstream-private-error";
        var request = chunkRequest(DocumentChunkStrategy.HYBRID, false);
        assertThatThrownBy(() -> parser.submitChunk(request))
                .isInstanceOfSatisfying(DocumentParseException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(503);
                    assertThat(exception.getMessage()).doesNotContain("private-error");
                });
        assertThat(paths).hasSize(1);
    }

    private DocumentChunkRequest chunkRequest(DocumentChunkStrategy strategy, boolean includeDocument) {
        var document = request("sample.pdf");
        DocumentChunkOptions options = switch (strategy) {
            case HIERARCHICAL -> new HierarchicalDocumentChunkOptions(true, true);
            case HYBRID -> new HybridDocumentChunkOptions(true, true, 128, "test-tokenizer", false);
        };
        return new DocumentChunkRequest(document, options, includeDocument);
    }

    private String chunkResponse(boolean includeDocument) throws Exception {
        ObjectNode response = (ObjectNode) json.readTree("""
                {"chunks":[{"filename":"sample.pdf","chunk_index":0,"text":"标题\\n正文","raw_text":"正文",
                 "num_tokens":12,"headings":["标题"],"captions":["表格说明"],"doc_items":["#/texts/0"],
                 "page_numbers":[1],"metadata":{"custom":"retained","nullable":null}}],
                 "documents":[],"processing_time":2.5}
                """);
        if (includeDocument) {
            JsonNode original = json.readTree(resultResponse("partial_success"));
            ObjectNode document = json.createObjectNode();
            document.put("kind", "ExportResult");
            document.set("content", original.path("document"));
            document.set("status", original.path("status"));
            document.set("errors", original.path("errors"));
            response.withArray("documents").add(document);
        }
        return json.writeValueAsString(response);
    }

    private DoclingDocumentParser createParser(int maxFileBytes) {
        Duration timeout = Duration.ofSeconds(2);
        return createParser(maxFileBytes, timeout);
    }

    private DoclingDocumentParser createParser(int maxFileBytes, Duration timeout) {
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        var http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER);
        var client = DoclingTaskClient.builder().baseUrl(base).apiKey("test-only-key")
                .connectTimeout(timeout).readTimeout(timeout)
                .httpClientBuilder(http).build();
        DoclingResultConverter converter = Mappers.getMapper(DoclingResultConverter.class);
        return new DoclingDocumentParser(client, converter, maxFileBytes);
    }

    private DocumentParseRequest request(String filename) {
        var resource = new ByteArrayResource("test document".getBytes(StandardCharsets.UTF_8));
        var languages = List.of("zh", "en");
        Duration timeout = Duration.ofSeconds(120);
        var options = new DocumentParseOptions(true, false, languages, true, 1, 2, timeout);
        return new DocumentParseRequest(resource, filename, options);
    }

    private void respond(HttpExchange exchange) throws IOException {
        paths.add(exchange.getRequestURI().toString());
        apiKey = exchange.getRequestHeaders().getFirst("X-Api-Key");
        byte[] input = exchange.getRequestBody().readAllBytes();
        if (input.length > 0) {
            requestBody = json.readTree(input);
        }
        byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        if (responseDelayMillis > 0) {
            try {
                Thread.sleep(responseDelayMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        if (redirect != null) {
            exchange.getResponseHeaders().set("Location", redirect);
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private String resultResponse(String status) {
        return """
                {"status":"%s","processing_time":1.5,
                 "errors":[{"error_message":"部分页面无法识别"}],
                 "document":{"md_content":"# 标题","json_content":{
                   "schema_name":"DoclingDocument","version":"1.7.0","name":"sample",
                   "body":{"self_ref":"#/body","label":"unspecified","children":[
                     {"$ref":"#/groups/0"},{"$ref":"#/texts/0"}]},
                   "groups":[{"self_ref":"#/groups/0","label":"section","children":[
                     {"$ref":"#/texts/1"},{"$ref":"#/tables/0"}]}],
                   "texts":[
                     {"self_ref":"#/texts/0","label":"text","text":"正文"},
                     {"self_ref":"#/texts/1","label":"section_header","text":"标题","level":2,
                      "parent":{"$ref":"#/groups/0"},"prov":[{"page_no":1,
                      "bbox":{"l":10,"t":780,"r":100,"b":760,"coord_origin":"BOTTOMLEFT"}}]}],
                   "tables":[{"self_ref":"#/tables/0","label":"table",
                     "data":{"num_rows":1,"num_cols":1,"table_cells":[{"text":"单元格","start_col_offset_idx":0,
                     "end_col_offset_idx":2,"column_header":true}]}}],
                   "pages":{"1":{"page_no":1,"size":{"width":600,"height":800}}}
                 }}}
                """.formatted(status);
    }
}
