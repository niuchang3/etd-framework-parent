package org.etd.ai.document;

import org.etd.ai.document.biz.KnowledgeDocumentBizService;
import org.etd.ai.document.entity.KnowledgeDocumentEntity;
import org.etd.ai.document.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.upload.component.MinioMultipartStorage;
import org.etd.ai.upload.service.StorageCleanupService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 管理详情放开状态筛选后，原件下载仍须执行后端启用状态校验。 */
class DocumentDownloadStatusTest {
    /** 被测试的下载编排入口。 */
    private KnowledgeDocumentBizService biz;
    /** 当前知识库的真实状态。 */
    private KnowledgeBaseEntity base;
    /** 已通过访问权限校验的文档。 */
    private KnowledgeDocumentEntity document;
    /** 存储调用替身，禁用状态不得触发签名。 */
    private MinioMultipartStorage storage;

    /** 准备已授权的文档及知识库，避免把状态限制与权限限制混为一谈。 */
    @BeforeEach
    void setUp() {
        var bases = mock(KnowledgeBaseService.class);
        var documents = mock(KnowledgeDocumentService.class);
        storage = mock(MinioMultipartStorage.class);
        base = new KnowledgeBaseEntity();
        base.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        document = new KnowledgeDocumentEntity();
        document.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        document.setStorageBucket("test");
        document.setStorageObjectKey("key");
        document.setOriginalFilename("a.txt");
        when(bases.requireBase(1L, false)).thenReturn(base);
        when(documents.requireReadable(1L, 101L)).thenReturn(document);
        biz = new KnowledgeDocumentBizService(bases, mock(KnowledgeFolderService.class), documents,
                mock(StorageCleanupService.class), storage);
    }

    /** 两者都启用时按既有流程签发原件访问链接。 */
    @Test
    void shouldSignEnabledDocument() throws Exception {
        when(storage.signDownload("test", "key", KnowledgeDocumentBizService.DOWNLOAD_EXPIRY_SECONDS, "a.txt"))
                .thenReturn("http://storage.invalid/download");
        assertEquals("http://storage.invalid/download", biz.createDownloadUrl(1L, 101L));
    }

    /** 任意一方停用都必须在调用存储前拒绝，不能依赖页面筛选。 */
    @ParameterizedTest
    @CsvSource({"0,1", "1,0", "0,0"})
    void shouldRejectDisabledBaseOrDocument(int baseStatus, int documentStatus) {
        base.setDataStatus(baseStatus);
        document.setDataStatus(documentStatus);
        assertThrows(ApiRuntimeException.class, () -> biz.createDownloadUrl(1L, 101L));
        verifyNoInteractions(storage);
    }
}
