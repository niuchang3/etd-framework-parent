package org.etd.ai.knowledge;

import io.minio.*;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.service.ModelService;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.etd.ai.knowledge.biz.*;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.*;
import org.etd.ai.knowledge.service.*;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 连接隔离测试库及独立MinIO桶，验证真实上传、续传、JSON回执、权限与处理状态。 */
@EnabledIfEnvironmentVariable(named = "AI_KNOWLEDGE_TEST_JDBC_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=knowledge-test",
        "spring.datasource.druid.url=${AI_KNOWLEDGE_TEST_JDBC_URL}",
        "spring.datasource.druid.username=${AI_KNOWLEDGE_TEST_DB_USER}",
        "spring.datasource.druid.password=${AI_KNOWLEDGE_TEST_DB_PASSWORD}",
        "spring.datasource.druid.driver-class-name=org.postgresql.Driver",
        "etd.ai.knowledge.upload.cleanup-enabled=false",
        "management.health.redis.enabled=false",
        "logging.file=${java.io.tmpdir}/etd-knowledge-integration-test"
})
class KnowledgeUploadIntegrationTest {
    /** 真实事务和数据权限代理。 */
    @Autowired private KnowledgeUploadBizService uploads;
    /** 真实目录编排。 */
    @Autowired private KnowledgeFolderBizService folderBiz;
    /** 真实资料查询。 */
    @Autowired private KnowledgeDocumentService documents;
    /** 真实资料管理。 */
    @Autowired private KnowledgeDocumentBizService documentBiz;
    /** 真实解析及索引处理。 */
    @Autowired private KnowledgeProcessingBizService processing;
    /** 正文查询。 */
    @Autowired private KnowledgeChunkService chunks;
    /** 隔离测试库查询。 */
    @Autowired private JdbcTemplate jdbc;
    /** 清理测试独立桶。 */
    @Autowired private CustomMinioClient minio;
    /** 过期清理使用的内部访问能力。 */
    @Autowired private KnowledgeUploadExpiryService expiry;
    /** 测试事务模板。 */
    @Autowired private TransactionTemplate transactions;
    /** 模型目录真实能力。 */
    @Autowired private ModelService models;
    /** 连接器真实能力，验证删除不再检查资料引用。 */
    @Autowired private ModelConnectionService connections;
    /** 生成局部模型响应。 */
    @Autowired private ObjectMapper json;
    /** 保留本测试创建的资料ID，失败时仍尝试清理。 */
    private final List<Long> created = new ArrayList<>();

    /** 使用组织权限用户，避免以管理员身份掩盖组织过滤问题。 */
    @BeforeEach
    void setIdentity() {
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        user.setScopeOrgIds(Set.of(10L));
        RequestContext.setUser(user);
        RequestContext.setTenantCode(900001L);
    }

    /** 清理本测试产生的原件，不触碰已有业务桶。 */
    @AfterEach
    void cleanObjects() {
        setIdentity();
        for (Long id : created) {
            try {
                documentBiz.removeDocument(id);
            } catch (Exception ignored) {
                // 断言失败时保留测试库中的记录便于定位；测试桶在外部测试流程最终清理。
            }
        }
        RequestContext.clean();
    }

    /** 初始化、真实PUT、回执确认、模拟重载续传、合并、解析、全文发布及删除完整走通。 */
    @Test
    void shouldResumeAndPublishDocument() throws Exception {
        var folder = createFolder();
        byte[] bytes = "中文知识资料与断点续传验证\n".repeat(80).getBytes(StandardCharsets.UTF_8);
        var document = createUpload(folder.getId(), bytes, "知识资料.md");
        assertEquals("uploading", document.getUploadStatus());
        assertEquals(500, document.getChunkMaxTokens());
        assertThrows(ApiRuntimeException.class, () -> uploads.createPartUrl(document.getId(), 2));
        String etag = putPart(document.getId(), 1, bytes);
        assertThrows(ApiRuntimeException.class, () -> uploads.confirmPart(document.getId(), 1, "invalid-etag"));
        uploads.confirmPart(document.getId(), 1, etag);
        uploads.confirmPart(document.getId(), 1, etag);
        var reloaded = documents.requireDocument(document.getId(), false);
        assertEquals(1, reloaded.getUploadParts().size());
        assertEquals(1, reloaded.getUploadParts().getFirst().partNumber());
        var resumed = uploads.resumeUpload(document.getId(), document.getFileSha256());
        assertEquals(1, resumed.getUploadParts().size());
        assertThrows(ApiRuntimeException.class, () -> uploads.resumeUpload(document.getId(), "0".repeat(64)));
        assertEquals("completed", uploads.completeUpload(document.getId()).getUploadStatus());
        assertEquals("completed", uploads.completeUpload(document.getId()).getUploadStatus());
        assertTrue(documentBiz.createDownloadUrl(document.getId()).contains("X-Amz"));
        var preview = processing.createPreview(document.getId());
        assertEquals("preview", preview.getProcessingStatus());
        assertFalse(chunks.fetchChunkList(document.getId(), preview.getProcessingVersion()).isEmpty());
        var ready = processing.approvePreview(document.getId());
        assertEquals("ready", ready.getProcessingStatus());
        assertEquals("ready", processing.approvePreview(document.getId()).getProcessingStatus());
        assertThrows(ApiRuntimeException.class, () -> folderBiz.removeFolder(folder.getId()));
        verifyPermissionIsolation(document.getId());
        documentBiz.removeDocument(document.getId());
        created.remove(document.getId());
        assertEquals(0, jdbc.queryForObject("select count(*) from ai_knowledge_chunk where document_id = ?", Integer.class, document.getId()));
        folderBiz.removeFolder(folder.getId());
    }

    /** 多个真实上传分片以乱序回执确认，再从持久化进度恢复上传。 */
    @Test
    void shouldPersistMultipartResumeAndCancel() throws Exception {
        var folder = createFolder();
        byte[] bytes = new byte[10 * 1024 * 1024 + 17];
        new Random(17).nextBytes(bytes);
        var document = createUpload(folder.getId(), bytes, "允许上传的任意格式.bin");
        byte[] second = Arrays.copyOfRange(bytes, 10 * 1024 * 1024, bytes.length);
        String secondEtag = putPart(document.getId(), 2, second);
        uploads.confirmPart(document.getId(), 2, secondEtag);
        assertThrows(ApiRuntimeException.class, () -> uploads.completeUpload(document.getId()));
        var resumed = uploads.resumeUpload(document.getId(), document.getFileSha256());
        assertEquals(2, resumed.getUploadParts().getFirst().partNumber());
        byte[] first = Arrays.copyOfRange(bytes, 0, 10 * 1024 * 1024);
        String firstEtag = putPart(document.getId(), 1, first);
        uploads.confirmPart(document.getId(), 1, firstEtag);
        assertEquals("completed", uploads.completeUpload(document.getId()).getUploadStatus());
        documentBiz.removeDocument(document.getId());
        created.remove(document.getId());
        var cancelled = createUpload(folder.getId(), second, "cancel.bin");
        uploads.cancelUpload(cancelled.getId());
        uploads.cancelUpload(cancelled.getId());
        assertThrows(ApiRuntimeException.class, () -> uploads.createPartUrl(cancelled.getId(), 1));
    }

    /** 并发确认不同分片不会丢失JSON清单；使用线程各自的可信上下文。 */
    @Test
    void shouldSerializeConcurrentPartConfirmations() throws Exception {
        var folder = createFolder();
        byte[] bytes = new byte[10 * 1024 * 1024 + 1];
        var document = createUpload(folder.getId(), bytes, "concurrent.bin");
        byte[] first = Arrays.copyOfRange(bytes, 0, 10 * 1024 * 1024);
        byte[] second = Arrays.copyOfRange(bytes, first.length, bytes.length);
        String firstEtag = putPart(document.getId(), 1, first);
        String secondEtag = putPart(document.getId(), 2, second);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstTask = executor.submit(() -> confirmPartWithIdentity(document.getId(), 1, firstEtag));
            var secondTask = executor.submit(() -> confirmPartWithIdentity(document.getId(), 2, secondEtag));
            firstTask.get();
            secondTask.get();
        }
        assertEquals(2, documents.requireDocument(document.getId(), false).getUploadParts().size());
        uploads.cancelUpload(document.getId());
    }

    /** 过期扫描无需用户上下文，但只处理已到期且未完成上传的资料。 */
    @Test
    void shouldCancelExpiredUploadsFromDocumentTable() throws Exception {
        var folder = createFolder();
        byte[] bytes = "expired".getBytes(StandardCharsets.UTF_8);
        var document = createUpload(folder.getId(), bytes, "expired.txt");
        jdbc.update("update ai_knowledge_document set upload_expires_at = current_timestamp - interval '1 minute' where id = ?", document.getId());
        RequestContext.clean();
        var objects = new KnowledgeUploadExpiryBizService(expiry, objectService, transactions);
        objects.cancelExpiredUploadList();
        setIdentity();
        assertEquals("cancelled", documents.requireDocument(document.getId(), false).getUploadStatus());
    }

    /** 使用本地HTTP替身而非真实付费模型，验证选择模型、维度、向量写入和连接器删除规则。 */
    @Test
    void shouldBuildVectorsAndAllowDeletingReferencedConnection() throws Exception {
        var address = new InetSocketAddress("127.0.0.1", 0);
        var server = HttpServer.create(address, 0);
        var embedding = new ArrayList<Double>();
        for (int index = 0; index < 768; index++) {
            embedding.add(index == 0 ? 1.0 : 0.0);
        }
        var vectorRow = Map.of("embedding", embedding);
        var rows = List.of(vectorRow);
        byte[] response = json.writeValueAsBytes(Map.of("data", rows));
        server.createContext("/embeddings", exchange -> {
            var request = json.readTree(exchange.getRequestBody());
            assertEquals(768, request.path("dimensions").asInt());
            assertEquals("Bearer test-only-key", exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            verifyVectorProcessing(server.getAddress().getPort());
        } finally {
            server.stop(0);
        }
    }

    /** 原件和正文走真实存储；模型连接凭证经过真实加解密流程。 */
    private void verifyVectorProcessing(int port) throws Exception {
        var folder = createFolder();
        byte[] bytes = "真实向量存储验证".getBytes(StandardCharsets.UTF_8);
        var document = createUpload(folder.getId(), bytes, "vector.txt");
        String etag = putPart(document.getId(), 1, bytes);
        uploads.confirmPart(document.getId(), 1, etag);
        uploads.completeUpload(document.getId());
        var model = new ModelEntity();
        model.setProviderCode("test");
        model.setModelType("embedding");
        model.setModelName("测试向量模型");
        model.setModelCode("test-" + UUID.randomUUID());
        model.setEmbedding(new Integer[] {768});
        model.setSort(0);
        RequestContext.getUser().setPlatformAdmin(true);
        models.createModel(model);
        RequestContext.getUser().setPlatformAdmin(false);
        var connection = new ModelConnectionEntity();
        connection.setModelId(model.getId());
        connection.setProtocol("openai_compatible");
        connection.setBaseUrl("http://127.0.0.1:" + port);
        connection.setAuthType("api_key");
        connections.createConnection(connection, "test-only-key");
        var options = connections.fetchEmbeddingConnectionOptionList(KnowledgePolicy.DIMENSIONS);
        var selected = options.stream().filter(option -> option.getId().equals(connection.getId())).findFirst().orElseThrow();
        assertArrayEquals(new Integer[] {768}, selected.getEmbedding());
        var input = documents.requireDocument(document.getId(), false);
        Long version = input.getVersion();
        input.setEmbeddingModelConnectionId(connection.getId());
        input.setEmbedding(768);
        input.setRetrievalMode("vector");
        documentBiz.modifyDocument(document.getId(), input, version);
        processing.createPreview(document.getId());
        assertEquals("ready", processing.approvePreview(document.getId()).getProcessingStatus());
        assertEquals(1, jdbc.queryForObject("select count(*) from ai_vector_768 where document_id = ?", Integer.class, document.getId()));
        connections.removeConnection(connection.getId());
        assertEquals("ready", documents.requireDocument(document.getId(), false).getProcessingStatus());
        processing.createPreview(document.getId());
        assertThrows(ApiRuntimeException.class, () -> processing.approvePreview(document.getId()));
        assertEquals("failed", documents.requireDocument(document.getId(), false).getProcessingStatus());
    }

    /** 每个工作线程独立设置并释放数据权限上下文。 */
    private void confirmPartWithIdentity(Long id, int number, String etag) {
        setIdentity();
        try {
            uploads.confirmPart(id, number, etag);
        } finally {
            RequestContext.clean();
        }
    }

    /** 复用真实对象存储能力以验证过期取消。 */
    @Autowired private KnowledgeObjectService objectService;
    /** 普通用户的组织范围及租户范围都在服务代理处执行。 */
    private void verifyPermissionIsolation(Long id) {
        RequestContext.getUser().setScopeOrgIds(Set.of(11L));
        assertThrows(ApiRuntimeException.class, () -> documents.requireDocument(id, false));
        RequestContext.getUser().setScopeOrgIds(Set.of(10L));
        RequestContext.setTenantCode(900002L);
        assertThrows(ApiRuntimeException.class, () -> documents.requireDocument(id, false));
        RequestContext.setTenantCode(900001L);
    }

    /** 创建独立目录，名称全局随机避免测试相互影响。 */
    private KnowledgeFolderEntity createFolder() {
        var folder = new KnowledgeFolderEntity();
        folder.setOrgId(10L);
        folder.setFolderName("集成测试-" + UUID.randomUUID());
        folderBiz.createFolder(folder);
        return folder;
    }

    /** 创建以完整摘要标识的上传记录。 */
    private KnowledgeDocumentEntity createUpload(Long folderId, byte[] bytes, String name) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        var document = new KnowledgeDocumentEntity();
        document.setOrgId(10L);
        document.setFolderId(folderId);
        document.setOriginalFilename(name);
        document.setContentType("application/octet-stream");
        document.setFileSize((long) bytes.length);
        document.setFileSha256(HexFormat.of().formatHex(digest.digest(bytes)));
        var result = uploads.createUpload(document);
        created.add(result.getId());
        return result;
    }

    /** 通过真实预签名地址上传字节，模拟浏览器PUT，不经业务服务器中转。 */
    private String putPart(Long id, int number, byte[] bytes) throws Exception {
        String url = uploads.createPartUrl(id, number);
        var body = HttpRequest.BodyPublishers.ofByteArray(bytes);
        var request = HttpRequest.newBuilder(URI.create(url)).PUT(body).build();
        var client = HttpClient.newHttpClient();
        var handler = HttpResponse.BodyHandlers.ofString();
        var response = client.send(request, handler);
        assertEquals(200, response.statusCode());
        return response.headers().firstValue("etag").orElseThrow();
    }
}
