package org.etd.framework.common.core.model;

import org.etd.framework.common.core.spring.SpringContextHelper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统一返回模型异常信息生成测试。
 */
class ResultModelTest {

    /**
     * Spring 容器尚未初始化时，失败响应仍应保留原始状态而不能抛出二次异常。
     */
    @Test
    void shouldCreateFailedResultWithoutApplicationContext() {
        ReflectionTestUtils.setField(SpringContextHelper.class, "context", null);

        ResultModel<Object> result = assertDoesNotThrow(
                () -> ResultModel.failed(401, new IllegalStateException("认证失败"), "未认证", "/api/events"));

        assertNull(result.getDevMessage());
    }
}
