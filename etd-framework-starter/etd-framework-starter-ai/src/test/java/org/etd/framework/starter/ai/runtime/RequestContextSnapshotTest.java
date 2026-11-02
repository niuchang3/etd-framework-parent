package org.etd.framework.starter.ai.runtime;

import java.util.concurrent.Executors;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.*;

/** 验证线程复用、异常及嵌套恢复，防止聊天并发串租户或遗留Token。 */
class RequestContextSnapshotTest {
    /** 测试后移除线程身份。 */
    @AfterEach
    void clear() {
        RequestContext.clean();
        SecurityContextHolder.clearContext();
    }

    /** 同一工作线程依次执行不同租户，异常退出后仍恢复工作线程原上下文。 */
    @Test
    void restoresContextAcrossReusedThreadAndFailure() throws Exception {
        UserDetails user = new UserDetails();
        user.setId(10L);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(1L);
        RequestContext.setToken("synthetic-user-token");
        RequestContext.setAttribute("x-business", "one");
        RequestContextSnapshot first = RequestContextSnapshot.capture();
        RequestContext.setTenantCode(2L);
        RequestContext.setToken("synthetic-second-token");
        RequestContextSnapshot second = RequestContextSnapshot.capture();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> task = executor.submit(() -> {
                try (Scope scope = first.openScope()) {
                    assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
                    assertThat(RequestContext.getToken()).isEqualTo("synthetic-user-token");
                    try (Scope nested = second.openScope()) {
                        assertThat(RequestContext.getTenantCode()).isEqualTo(2L);
                    }
                    assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
                }
                try {
                    try (Scope scope = first.openScope()) {
                        throw new IllegalStateException("test");
                    }
                } catch (IllegalStateException failure) {
                    assertThat(failure).hasMessage("test");
                }
                assertThat(RequestContext.getTenantCode()).isNull();
                assertThat(RequestContext.getToken()).isNull();
            });
            task.get();
        }
        assertThat(RequestContext.getTenantCode()).isEqualTo(2L);
    }
}
