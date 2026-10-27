package org.etd.ai.knowledge;

import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 验证新增资料在写库前拒绝缺少组织归属的请求，权限放行不能绕过必填校验。 */
class KnowledgeDocumentCreateTest {
    /** 数据访问替身，用于确认非法资料不会进入数据库。 */
    private final KnowledgeDocumentMapper mapper = mock(KnowledgeDocumentMapper.class);
    /** 真实资料新增能力。 */
    private final KnowledgeDocumentService service = new KnowledgeDocumentService(mapper);

    /** 释放线程身份，避免影响其他测试。 */
    @AfterEach
    void clearIdentity() {
        RequestContext.clean();
    }

    /** 全部数据范围在组织缺失时同样返回明确业务提示。 */
    @Test
    void shouldRejectMissingOrganizationWithAllDataPermission() {
        setIdentity(BasicConstant.PermissionType.ALL.getCode(), null);
        var document = new KnowledgeDocumentEntity();
        var exception = assertThrows(ApiRuntimeException.class, () -> service.createDocument(document));
        assertEquals("资料缺少知识库组织归属，无法上传", exception.getMessage());
        verifyNoInteractions(mapper);
    }

    /** 本人数据范围不能让无组织资料落库。 */
    @Test
    void shouldRejectMissingOrganizationWithSelfPermission() {
        setIdentity(BasicConstant.PermissionType.SELF.getCode(), null);
        var document = new KnowledgeDocumentEntity();
        assertThrows(ApiRuntimeException.class, () -> service.createDocument(document));
        verifyNoInteractions(mapper);
    }

    /** 资料缺少知识库组织时不能改用上传人的主组织。 */
    @Test
    void shouldRejectMissingOrganizationEvenWithPrimaryOrganization() {
        setIdentity(BasicConstant.PermissionType.SELF.getCode(), 10L);
        var document = new KnowledgeDocumentEntity();
        assertThrows(ApiRuntimeException.class, () -> service.createDocument(document));
        verifyNoInteractions(mapper);
    }

    /** 内部调用已明确归属组织时仍按原有权限规则写入。 */
    @Test
    void shouldAllowExplicitOrganization() {
        setIdentity(BasicConstant.PermissionType.ALL.getCode(), null);
        var document = new KnowledgeDocumentEntity();
        document.setOrgId(10L);
        service.createDocument(document);
        verify(mapper).insert(document);
    }

    /** 创建具有指定数据范围和可选主组织的可信身份。 */
    private void setIdentity(String permissionType, Long orgId) {
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(orgId);
        var types = Set.of(permissionType);
        user.setPermissionTypes(types);
        RequestContext.setUser(user);
    }
}
