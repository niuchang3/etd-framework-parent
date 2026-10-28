package org.etd.ai.knowledge;

import org.etd.ai.knowledge.biz.KnowledgeUploadBizService;
import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentListVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentVO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证上传时生成文件展示类型，且类型随资料详情和列表返回。 */
class KnowledgeUploadFileTypeTest {
    private KnowledgeUploadBizService uploads;
    private KnowledgeDocumentService documents;

    /** 释放组织继承测试使用的可信身份。 */
    @AfterEach
    void clearIdentity() {
        RequestContext.clean();
    }

    /** 使用真实事务模板执行上传编排，隔离数据库和对象存储IO。 */
    @BeforeEach
    void setUp() throws Exception {
        KnowledgeFolderService folders = mock(KnowledgeFolderService.class);
        documents = mock(KnowledgeDocumentService.class);
        KnowledgeObjectService objects = mock(KnowledgeObjectService.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        TransactionTemplate transactions = new TransactionTemplate(manager);
        KnowledgeUploadProperties properties = new KnowledgeUploadProperties();
        KnowledgeFolderEntity folder = new KnowledgeFolderEntity();
        folder.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        KnowledgeBaseService bases = mock(KnowledgeBaseService.class);
        KnowledgeBaseEntity base = new KnowledgeBaseEntity();
        base.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        base.setOrgId(10L);
        base.setIsPublic(false);
        when(bases.requireBase(1L, true, false)).thenReturn(base);
        when(folders.requireFolderByBase(1L, 1L)).thenReturn(folder);
        when(objects.initUpload(any())).thenReturn("test-upload");
        uploads = new KnowledgeUploadBizService(folders, bases, documents, objects, properties, transactions);
    }

    /** 大小写、复合扩展名及无扩展名按统一规则保存，不采信预置类型或MIME声明。 */
    @ParameterizedTest
    @CsvSource({"说明.PDF,pdf", "报告.DocX,docx", "表格.XLSX,xlsx", "归档.tar.gz,gz",
            "README,unknown", "文件.,unknown", ".env,unknown"})
    void shouldGenerateFileTypeFromOriginalFilename(String filename, String expected) {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setFolderId(1L);
        document.setKnowledgeBaseId(1L);
        document.setOriginalFilename(filename);
        document.setFileSize(10L);
        document.setContentType("application/octet-stream");
        document.setFileType("untrusted");
        KnowledgeDocumentEntity result = uploads.createUpload(document);
        assertEquals(expected, result.getFileType());
        assertEquals(KnowledgePolicy.UPLOADING, result.getUploadStatus());
        verify(documents).createDocument(document);
        KnowledgeDocumentVO detail = KnowledgeDocumentConverter.INSTANCE.toVO(result);
        KnowledgeDocumentListVO list = KnowledgeDocumentConverter.INSTANCE.toListVO(result);
        assertEquals(expected, detail.getFileType());
        assertEquals(expected, list.getFileType());
    }

    /** 上传人无主组织或主组织不同，均按知识库组织通过真实新增权限校验。 */
    @ParameterizedTest
    @NullSource
    @ValueSource(longs = 20L)
    void shouldInheritKnowledgeBaseOrganization(Long primaryOrgId) {
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setOrgId(primaryOrgId);
        Set<String> types = Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode());
        user.setPermissionTypes(types);
        Set<Long> scopeOrgIds = Set.of(10L);
        user.setScopeOrgIds(scopeOrgIds);
        RequestContext.setUser(user);
        KnowledgeDocumentMapper mapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeDocumentService service = new KnowledgeDocumentService(mapper);
        doAnswer(invocation -> service.createDocument(invocation.getArgument(0)))
                .when(documents).createDocument(any());
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setKnowledgeBaseId(1L);
        document.setFolderId(1L);
        document.setOriginalFilename("资料.pdf");
        document.setFileSize(10L);
        // 即使内部调用传入其他组织，也必须以已授权知识库为准。
        document.setOrgId(99L);
        KnowledgeDocumentEntity result = uploads.createUpload(document);
        assertEquals(10L, result.getOrgId());
        verify(mapper).insert(document);
    }
}
