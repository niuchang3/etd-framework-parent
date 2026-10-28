package org.etd.ai.knowledge;

import io.minio.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.Executors;
import org.etd.ai.knowledge.biz.*;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.etd.ai.knowledge.biz.KnowledgeUploadExpiryBizService;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import static org.junit.jupiter.api.Assertions.*;

/** 连接隔离测试库及独立MinIO桶，验证真实上传、续传、JSON回执、权限与上传状态。 */
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
    /** 隔离测试库查询。 */
    @Autowired private JdbcTemplate jdbc;
    /** 过期清理使用的内部访问能力。 */
    @Autowired private KnowledgeUploadExpiryService expiry;
    /** 测试事务模板。 */
    @Autowired private TransactionTemplate transactions;
    /** 保留本测试创建的资料ID，失败时仍尝试清理。 */
    private final List<Long> created = new ArrayList<>();

    /** 使用组织权限用户，避免以管理员身份掩盖组织过滤问题。 */
    @BeforeEach
    void setIdentity() {
        UserDetails user = new UserDetails();
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

    /** 初始化、真实PUT、回执确认、模拟重载续传、合并及删除完整走通。 */
    @Test
    void shouldResumeAndCompleteUpload() throws Exception {
        KnowledgeFolderEntity folder = createFolder();
        byte[] bytes = "中文知识资料与断点续传验证\n".repeat(80).getBytes(StandardCharsets.UTF_8);
        KnowledgeDocumentEntity document = createUpload(folder.getId(), bytes, "知识资料.md");
        assertEquals("uploading", document.getUploadStatus());
        assertThrows(ApiRuntimeException.class, () -> uploads.createPartUrl(document.getId(), 2));
        String etag = putPart(document.getId(), 1, bytes);
        assertThrows(ApiRuntimeException.class, () -> uploads.confirmPart(document.getId(), 1, "invalid-etag"));
        uploads.confirmPart(document.getId(), 1, etag);
        uploads.confirmPart(document.getId(), 1, etag);
        KnowledgeDocumentEntity reloaded = documents.requireDocument(document.getId(), false);
        assertEquals(1, reloaded.getUploadParts().size());
        assertEquals(1, reloaded.getUploadParts().getFirst().partNumber());
        KnowledgeDocumentEntity resumed = uploads.resumeUpload(document.getId(), document.getFileSha256());
        assertEquals(1, resumed.getUploadParts().size());
        assertThrows(ApiRuntimeException.class, () -> uploads.resumeUpload(document.getId(), "0".repeat(64)));
        assertEquals("completed", uploads.completeUpload(document.getId()).getUploadStatus());
        assertEquals("completed", uploads.completeUpload(document.getId()).getUploadStatus());
        verifyOriginalDownload(document.getId(), "知识资料.md", bytes);
        KnowledgeDocumentEntity completed = documents.requireDocument(document.getId(), false);
        assertEquals("completed", completed.getUploadStatus());
        assertNotNull(completed.getUploadedTime());
        assertEquals("知识资料.md", completed.getDocumentName());
        assertEquals("md", completed.getFileType());
        assertEquals((long) bytes.length, completed.getFileSize());
        assertThrows(ApiRuntimeException.class, () -> folderBiz.removeFolder(folder.getId()));
        verifyPermissionIsolation(document.getId());
        documentBiz.removeDocument(document.getId());
        created.remove(document.getId());
        folderBiz.removeFolder(folder.getId());
    }

    /** 验证真实对象存储响应保留原始中文文件名、扩展名和原件字节。 */
    private void verifyOriginalDownload(Long id, String filename, byte[] expected) throws Exception {
        String url = documentBiz.createDownloadUrl(id);
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse.BodyHandler<byte[]> handler = HttpResponse.BodyHandlers.ofByteArray();
        HttpResponse<byte[]> response = client.send(request, handler);
        assertEquals(200, response.statusCode());
        String header = response.headers().firstValue(HttpHeaders.CONTENT_DISPOSITION).orElseThrow();
        ContentDisposition disposition = ContentDisposition.parse(header);
        assertEquals(filename, disposition.getFilename());
        assertArrayEquals(expected, response.body());
    }

    /** 多个真实上传分片以乱序回执确认，再从持久化进度恢复上传。 */
    @Test
    void shouldPersistMultipartResumeAndCancel() throws Exception {
        KnowledgeFolderEntity folder = createFolder();
        byte[] bytes = new byte[10 * 1024 * 1024 + 17];
        new Random(17).nextBytes(bytes);
        KnowledgeDocumentEntity document = createUpload(folder.getId(), bytes, "允许上传的任意格式.bin");
        byte[] second = Arrays.copyOfRange(bytes, 10 * 1024 * 1024, bytes.length);
        String secondEtag = putPart(document.getId(), 2, second);
        uploads.confirmPart(document.getId(), 2, secondEtag);
        assertThrows(ApiRuntimeException.class, () -> uploads.completeUpload(document.getId()));
        KnowledgeDocumentEntity resumed = uploads.resumeUpload(document.getId(), document.getFileSha256());
        assertEquals(2, resumed.getUploadParts().getFirst().partNumber());
        byte[] first = Arrays.copyOfRange(bytes, 0, 10 * 1024 * 1024);
        String firstEtag = putPart(document.getId(), 1, first);
        uploads.confirmPart(document.getId(), 1, firstEtag);
        assertEquals("completed", uploads.completeUpload(document.getId()).getUploadStatus());
        documentBiz.removeDocument(document.getId());
        created.remove(document.getId());
        KnowledgeDocumentEntity cancelled = createUpload(folder.getId(), second, "cancel.bin");
        uploads.cancelUpload(cancelled.getId());
        uploads.cancelUpload(cancelled.getId());
        assertThrows(ApiRuntimeException.class, () -> uploads.createPartUrl(cancelled.getId(), 1));
    }

    /** 并发确认不同分片不会丢失JSON清单；使用线程各自的可信上下文。 */
    @Test
    void shouldSerializeConcurrentPartConfirmations() throws Exception {
        KnowledgeFolderEntity folder = createFolder();
        byte[] bytes = new byte[10 * 1024 * 1024 + 1];
        KnowledgeDocumentEntity document = createUpload(folder.getId(), bytes, "concurrent.bin");
        byte[] first = Arrays.copyOfRange(bytes, 0, 10 * 1024 * 1024);
        byte[] second = Arrays.copyOfRange(bytes, first.length, bytes.length);
        String firstEtag = putPart(document.getId(), 1, first);
        String secondEtag = putPart(document.getId(), 2, second);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> firstTask = executor.submit(() -> confirmPartWithIdentity(document.getId(), 1, firstEtag));
            Future<?> secondTask = executor.submit(() -> confirmPartWithIdentity(document.getId(), 2, secondEtag));
            firstTask.get();
            secondTask.get();
        }
        assertEquals(2, documents.requireDocument(document.getId(), false).getUploadParts().size());
        uploads.cancelUpload(document.getId());
    }

    /** 过期扫描无需用户上下文，但只处理已到期且未完成上传的资料。 */
    @Test
    void shouldCancelExpiredUploadsFromDocumentTable() throws Exception {
        KnowledgeFolderEntity folder = createFolder();
        byte[] bytes = "expired".getBytes(StandardCharsets.UTF_8);
        KnowledgeDocumentEntity document = createUpload(folder.getId(), bytes, "expired.txt");
        jdbc.update("update ai_knowledge_document set upload_expires_at = current_timestamp - interval '1 minute' where id = ?", document.getId());
        RequestContext.clean();
        KnowledgeUploadExpiryBizService objects = new KnowledgeUploadExpiryBizService(expiry, objectService, transactions);
        objects.cancelExpiredUploadList();
        setIdentity();
        assertEquals("cancelled", documents.requireDocument(document.getId(), false).getUploadStatus());
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
        jdbc.update("insert into ai_knowledge_base (id, tenant_id, org_id, create_by, name) values (10, 900001, 10, 100, '上传测试库') on conflict (id) do nothing");
        KnowledgeFolderEntity folder = new KnowledgeFolderEntity();
        folder.setKnowledgeBaseId(10L);
        folder.setFolderName("集成测试-" + UUID.randomUUID());
        folderBiz.createFolder(folder);
        return folder;
    }

    /** 创建以完整摘要标识的上传记录。 */
    private KnowledgeDocumentEntity createUpload(Long folderId, byte[] bytes, String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setOrgId(10L);
        document.setKnowledgeBaseId(10L);
        document.setFolderId(folderId);
        document.setOriginalFilename(name);
        document.setContentType("application/octet-stream");
        document.setFileSize((long) bytes.length);
        document.setFileSha256(HexFormat.of().formatHex(digest.digest(bytes)));
        KnowledgeDocumentEntity result = uploads.createUpload(document);
        created.add(result.getId());
        return result;
    }

    /** 通过真实预签名地址上传字节，模拟浏览器PUT，不经业务服务器中转。 */
    private String putPart(Long id, int number, byte[] bytes) throws Exception {
        String url = uploads.createPartUrl(id, number);
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofByteArray(bytes);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).PUT(body).build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        HttpResponse<String> response = client.send(request, handler);
        assertEquals(200, response.statusCode());
        return response.headers().firstValue("etag").orElseThrow();
    }
}
