package org.etd.framework.common.core.context;

import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

/**
 * RequestContextInitializer 全量上下文导出与初始化无损还原单元测试
 *
 * @author Young
 */
public class RequestContextInitializerTest {

    @BeforeEach
    public void setUp() {
        RequestContext.clean();
    }

    @AfterEach
    public void tearDown() {
        RequestContext.clean();
    }

    @Test
    @DisplayName("测试 RequestContextModel 全量上下文（headers, userDetails）的导出与还原")
    public void testFullContextExportAndInit() {
        // 1. 准备并初始化 ThreadLocal 上下文
        RequestContext.setTraceId("test-trace-123456");
        RequestContext.setTenantCode(8888L);
        RequestContext.setLanguage("zh-CN");
        RequestContext.setToken("Bearer my-secret-token");

        // 设置安全与用户身份领域模型
        UserDetails userDetails = new UserDetails();
        userDetails.setId(99L);
        userDetails.setAccount("test_user");
        userDetails.setUserName("测试用户");
        userDetails.setPassword("sensitive_password_123");
        userDetails.setRoleCodes(Set.of("ROLE_ADMIN", "ROLE_USER"));
        userDetails.setTenantId(8888L);
        userDetails.setOrgId(1001L);
        RequestContext.setUser(userDetails);

        // 2. 导出全量上下文到 Map
        Map<String, Object> exportedMap = RequestContextInitializer.exportMessageHeaders();

        // 校验导出内容
        Assertions.assertNotNull(exportedMap);
        Assertions.assertEquals("test-trace-123456", exportedMap.get(HeaderConstant.TRACE_ID));
        Assertions.assertEquals(8888L, exportedMap.get(HeaderConstant.TENANT_CODE));
        Assertions.assertEquals("zh-CN", exportedMap.get(HeaderConstant.ACCEPT_LANGUAGE));
        // Token 必须在导出消息头时安全清理
        Assertions.assertNull(exportedMap.get(HeaderConstant.AUTHORIZATION));

        // 校验 UserDetails 序列化导出
        Assertions.assertNotNull(exportedMap.get(HeaderConstant.InternalHeader.X_USER_DETAILS));
        String userJson = String.valueOf(exportedMap.get(HeaderConstant.InternalHeader.X_USER_DETAILS));
        Assertions.assertFalse(userJson.contains("sensitive_password_123"));

        // 3. 清理当前线程上下文，模拟 MQ / EventBus / 异步线程接收环境
        RequestContext.clean();
        Assertions.assertNull(RequestContext.getTraceId());
        Assertions.assertNull(RequestContext.getUser());

        // 4. 调用 init(exportedMap) 进行全量无损还原
        RequestContextInitializer.init(exportedMap);

        // 5. 验证还原结果
        Assertions.assertEquals("test-trace-123456", RequestContext.getTraceId());
        Assertions.assertEquals(8888L, RequestContext.getTenantCode());
        Assertions.assertEquals("zh-CN", RequestContext.getLanguage());

        // 验证 UserDetails 领域模型还原
        UserDetails restoredUser = RequestContext.getUser();
        Assertions.assertNotNull(restoredUser);
        Assertions.assertEquals(99L, restoredUser.getId());
        Assertions.assertEquals("test_user", restoredUser.getAccount());
        Assertions.assertEquals("测试用户", restoredUser.getUserName());
        Assertions.assertNull(restoredUser.getPassword());
        Assertions.assertTrue(restoredUser.getRoleCodes().contains("ROLE_ADMIN"));
        Assertions.assertEquals(1001L, restoredUser.getOrgId());
    }
}
