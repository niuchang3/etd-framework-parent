package org.etd.ai.knowledge;

import org.etd.ai.knowledge.service.KnowledgeCreationPolicy;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** 创建策略只保留插入所需的归属和创建范围，不代替查询权限拦截。 */
class KnowledgeCreationPolicyTest {
    /** 准备具备主组织的可信登录用户。 */
    @BeforeEach
    void setUp() {
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setPermissionTypes(Set.of());
        RequestContext.setUser(user);
        RequestContext.setTenantCode(1L);
    }

    /** 清理线程上下文。 */
    @AfterEach
    void cleanContext() {
        RequestContext.clean();
    }

    /** 本人范围允许创建自己的组织资料。 */
    @Test
    void shouldAllowSelfCreation() {
        RequestContext.getUser().setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        assertDoesNotThrow(KnowledgeCreationPolicy::requireOrganization);
    }

    /** 无角色范围不能创建，部门范围只允许在授权主组织创建。 */
    @Test
    void shouldUseFrameworkOrganizationScope() {
        assertThrows(ApiRuntimeException.class, KnowledgeCreationPolicy::requireOrganization);
        RequestContext.getUser().setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        RequestContext.getUser().setScopeOrgIds(Set.of(20L));
        assertThrows(ApiRuntimeException.class, KnowledgeCreationPolicy::requireOrganization);
        RequestContext.getUser().setScopeOrgIds(Set.of(10L));
        assertDoesNotThrow(KnowledgeCreationPolicy::requireOrganization);
    }

    /** 管理员不受数据范围限制，但新增仍需明确数据库记录的归属字段。 */
    @Test
    void shouldAllowAdminWithExplicitCreationOwnership() {
        RequestContext.getUser().setPlatformAdmin(true);
        assertDoesNotThrow(KnowledgeCreationPolicy::requireOrganization);
        RequestContext.setTenantCode(null);
        assertThrows(ApiRuntimeException.class, KnowledgeCreationPolicy::requireOrganization);
    }
}
