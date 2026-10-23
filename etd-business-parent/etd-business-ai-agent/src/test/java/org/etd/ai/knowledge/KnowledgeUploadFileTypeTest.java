package org.etd.ai.knowledge;

import org.etd.ai.knowledge.biz.KnowledgeUploadBizService;
import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证上传时生成文件展示类型，且类型随资料详情和列表返回。 */
class KnowledgeUploadFileTypeTest {
    private KnowledgeUploadBizService uploads;
    private KnowledgeDocumentService documents;

    /** 使用真实事务模板执行上传编排，隔离数据库和对象存储IO。 */
    @BeforeEach
    void setUp() throws Exception {
        var folders = mock(KnowledgeFolderService.class);
        documents = mock(KnowledgeDocumentService.class);
        var objects = mock(KnowledgeObjectService.class);
        var manager = mock(PlatformTransactionManager.class);
        var status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        var transactions = new TransactionTemplate(manager);
        var properties = new KnowledgeUploadProperties();
        var folder = new KnowledgeFolderEntity();
        folder.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        when(folders.requireWritable(1L)).thenReturn(folder);
        when(objects.initUpload(any())).thenReturn("test-upload");
        uploads = new KnowledgeUploadBizService(folders, documents, objects, properties, transactions);
    }

    /** 大小写、复合扩展名及无扩展名按统一规则保存，不采信预置类型或MIME声明。 */
    @ParameterizedTest
    @CsvSource({"说明.PDF,pdf", "报告.DocX,docx", "表格.XLSX,xlsx", "归档.tar.gz,gz",
            "README,unknown", "文件.,unknown", ".env,unknown"})
    void shouldGenerateFileTypeFromOriginalFilename(String filename, String expected) {
        var document = new KnowledgeDocumentEntity();
        document.setFolderId(1L);
        document.setOriginalFilename(filename);
        document.setFileSize(10L);
        document.setContentType("application/octet-stream");
        document.setFileType("untrusted");
        var result = uploads.createUpload(document);
        assertEquals(expected, result.getFileType());
        assertEquals(KnowledgePolicy.UPLOADING, result.getUploadStatus());
        verify(documents).createDocument(document);
        var detail = KnowledgeDocumentConverter.INSTANCE.toVO(result);
        var list = KnowledgeDocumentConverter.INSTANCE.toListVO(result);
        assertEquals(expected, detail.getFileType());
        assertEquals(expected, list.getFileType());
    }
}
