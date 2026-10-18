package org.etd.ai.upload;

import org.etd.ai.document.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.upload.biz.UploadBizService;
import org.etd.ai.upload.component.MinioMultipartStorage;
import org.etd.ai.upload.component.UploadedFileInspector;
import org.etd.ai.upload.constant.MultipartUploadLimits;
import org.etd.ai.upload.controller.UploadController;
import org.etd.ai.upload.entity.UploadSessionEntity;
import org.etd.ai.upload.service.StorageCleanupService;
import org.etd.ai.upload.service.UploadSessionService;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证上传不再读取UPMS策略，客户端传输参数可以经过真实Controller保存为任务快照。 */
class UploadClientParametersTest {
    /** 真实上传编排入口，数据库及存储使用替身。 */
    private UploadBizService biz;
    /** 用于核对实际持久化任务的会话能力替身。 */
    private UploadSessionService sessions;
    /** 用于核对签名有效期及初始化调用的存储替身。 */
    private MinioMultipartStorage storage;
    /** 包含真实DTO校验和请求转换的HTTP测试入口。 */
    private MockMvc mvc;

    /** 准备可信管理员身份；上传业务全程不需要参数服务地址或HTTP客户端。 */
    @BeforeEach
    void prepareUploadContext() {
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setPlatformAdmin(true);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(1L);
        sessions = mock(UploadSessionService.class);
        storage = mock(MinioMultipartStorage.class);
        var bases = mock(KnowledgeBaseService.class);
        var folders = mock(KnowledgeFolderService.class);
        var documents = mock(KnowledgeDocumentService.class);
        var cleanups = mock(StorageCleanupService.class);
        var inspector = new UploadedFileInspector();
        biz = new UploadBizService(sessions, storage, inspector, bases, folders, documents, cleanups);
        var controller = new UploadController(biz);
        var exceptions = new GlobalExceptionHandler();
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(exceptions).build();
    }

    /** 请求上下文不应跨测试保留。 */
    @AfterEach
    void clearUploadContext() {
        RequestContext.clean();
    }

    /** 其他扩展名及超过旧100MiB限制的文件仍可初始化，客户端参数必须原样保存。 */
    @Test
    void shouldAcceptClientParametersWithoutFilePolicyRestrictions() throws Exception {
        when(storage.requireBucket()).thenReturn("test");
        when(storage.initialize(any())).thenReturn("remote-upload");
        String body = """
                {"originalFilename":"data.archive","fileSize":104857601,
                 "partSizeBytes":8388608,"concurrency":17,"maxRetries":11,
                 "urlExpirySeconds":7200,"sessionExpireSeconds":86400}
                """;
        var request = post("/v1/knowledge-base/1/uploads")
                .contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isOk());
        var saved = ArgumentCaptor.forClass(UploadSessionEntity.class);
        verify(sessions).createSession(saved.capture());
        var session = saved.getValue();
        assertEquals("data.archive", session.getOriginalFilename());
        assertEquals(104857601L, session.getFileSize());
        assertEquals(8388608L, session.getPartSizeBytes());
        assertEquals(13, session.getPartCount());
        assertEquals(17, session.getConcurrency());
        assertEquals(11, session.getMaxRetries());
        assertEquals(7200, session.getUrlExpirySeconds());
        assertTrue(session.getExpiresAt().isAfter(Instant.now().plusSeconds(86300)));
        verify(storage).initialize(session);
    }

    /** 缺失客户端传输参数时明确返回400，不使用旧参数或服务端默认值兜底。 */
    @Test
    void shouldRejectMissingTransferParameters() throws Exception {
        String body = """
                {"originalFilename":"data.archive","fileSize":104857601}
                """;
        var request = post("/v1/knowledge-base/1/uploads")
                .contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(sessions, storage);
    }

    /** 移除业务限制不改变S3对普通分片最低尺寸的协议要求。 */
    @Test
    void shouldRejectPartSizeBelowProtocolMinimum() throws Exception {
        String body = """
                {"originalFilename":"data.archive","fileSize":104857601,
                 "partSizeBytes":1024,"concurrency":3,"maxRetries":3,
                 "urlExpirySeconds":900,"sessionExpireSeconds":86400}
                """;
        var request = post("/v1/knowledge-base/1/uploads")
                .contentType(MediaType.APPLICATION_JSON).content(body);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(sessions, storage);
    }

    /** 超过S3总片数的请求在创建远端任务前拒绝，避免产生不可完成的任务。 */
    @Test
    void shouldRejectPartCountBeyondProtocolCapacity() {
        var session = new UploadSessionEntity();
        session.setFileSize(MultipartUploadLimits.MIN_PART_BYTES * MultipartUploadLimits.MAX_PART_COUNT + 1);
        session.setPartSizeBytes(MultipartUploadLimits.MIN_PART_BYTES);
        assertThrows(ApiRuntimeException.class, () -> biz.createUpload(1L, session, 86400));
        verifyNoInteractions(sessions, storage);
    }

    /** 较长客户端任务有效期不能在转换为签名秒数时发生int溢出。 */
    @Test
    void shouldCapSignatureByRequestedExpiryForLongSession() throws Exception {
        var session = new UploadSessionEntity();
        session.setPartCount(1);
        session.setUrlExpirySeconds(7200);
        session.setExpiresAt(Instant.now().plusSeconds(Integer.MAX_VALUE));
        when(sessions.requireSession(1L, 2L, false)).thenReturn(session);
        when(storage.signPart(session, 1, 7200)).thenReturn("signed-part-url");
        assertEquals("signed-part-url", biz.createPartUrl(1L, 2L, 1));
    }
}
