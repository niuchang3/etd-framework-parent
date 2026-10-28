package org.etd.ai.knowledge;
import jakarta.validation.Validation;
import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import jakarta.validation.ValidatorFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/** 分类不再有组织权限字段，所有维护校验依赖所属知识库。 */
class KnowledgeFolderPersistenceTest {
    /** 禁止将目录挂到其他知识库，即使两个库均可维护。 */
    @Test
    void shouldRejectCrossBaseParent() {
        KnowledgeFolderMapper mapper = mock(KnowledgeFolderMapper.class);
        KnowledgeFolderService service = new KnowledgeFolderService(mapper);
        KnowledgeFolderEntity folder = new KnowledgeFolderEntity();
        folder.setKnowledgeBaseId(10L);
        when(mapper.selectWritable(1L)).thenReturn(folder);
        assertThrows(ApiRuntimeException.class, () -> service.requireParent(1L, 20L));
        service.requireParent(1L, 10L);
    }
    /** 新增目录必须明确知识库，不能继续提交orgId作为分类权限。 */
    @Test
    void shouldRequireKnowledgeBaseId() {
        KnowledgeFolderSaveDTO dto = new KnowledgeFolderSaveDTO();
        dto.setFolderName("产品资料");
        dto.setSortOrder(0);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(dto).isEmpty());
            dto.setKnowledgeBaseId(1L);
            assertTrue(factory.getValidator().validate(dto).isEmpty());
        }
    }
}
