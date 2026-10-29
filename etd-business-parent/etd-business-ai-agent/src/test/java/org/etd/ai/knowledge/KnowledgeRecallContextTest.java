package org.etd.ai.knowledge;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.knowledge.service.KnowledgeRecallService;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证知识库二次并发边界透传完整上下文，且不同请求之间不串租户或身份。 */
class KnowledgeRecallContextTest {
    /** 权限读取替身，模拟依赖当前租户和用户的数据库入口。 */
    private KnowledgeBaseService bases;
    /** 使用真实业务实现触发内部虚拟线程。 */
    private KnowledgeRecallBizService biz;

    /** 使用关键词召回隔离外部模型，逐个数据库入口核对租户、认证与请求头。 */
    @BeforeEach
    void initialize() {
        bases = mock(KnowledgeBaseService.class);
        KnowledgeRecallService recall = mock(KnowledgeRecallService.class);
        KnowledgeModelSettingsService settings = mock(KnowledgeModelSettingsService.class);
        ModelInferenceService inference = mock(ModelInferenceService.class);
        biz = new KnowledgeRecallBizService(bases, recall, settings, inference);
        when(bases.requireBase(anyLong(), eq(false), eq(false))).thenAnswer(call -> {
            Long id = call.getArgument(0);
            assertContext(id / 10);
            RequestContext.setAttribute("child-base", id);
            KnowledgeBaseEntity base = new KnowledgeBaseEntity();
            base.setId(id);
            base.setVersion(0L);
            base.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
            base.setRetrievalMode(KnowledgeRetrievalPolicy.KEYWORD);
            base.setTopK(1);
            base.setContextWindowSize(0);
            return base;
        });
        when(recall.fetchKeywordList(any(), anyString(), eq(true))).thenReturn(List.of());
        when(recall.mergeCandidateList(anyList(), anyList(), eq(false))).thenReturn(List.of());
        when(recall.retainCurrentList(anyLong(), anyList(), eq(true))).thenReturn(List.of());
        when(recall.expandContextWindow(anyList(), eq(0), eq(true))).thenReturn(List.of());
    }

    /** 退出后移除合成身份及日志上下文。 */
    @AfterEach
    void clear() {
        RequestContext.clean();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    /** 子线程修改扩展头不能影响调用方，内部首次读取和最终权限复核均须携带身份。 */
    @Test
    void propagatesContextToEveryKnowledgeTask() {
        recallForTenant(1L);
        verify(bases, times(2)).requireBase(11L, false, false);
        verify(bases, times(2)).requireBase(12L, false, false);
    }

    /** 两个租户同时进入多库检索，内部任务必须使用各自捕获的请求上下文。 */
    @Test
    void isolatesConcurrentTenantRequests() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Void> first = () -> recallForTenant(1L);
            Callable<Void> second = () -> recallForTenant(2L);
            Future<Void> firstResult = executor.submit(first);
            Future<Void> secondResult = executor.submit(second);
            firstResult.get(5, TimeUnit.SECONDS);
            secondResult.get(5, TimeUnit.SECONDS);
        }
        assertThat(RequestContext.getTenantCode()).isNull();
    }

    /** 补齐上下文不意味着绕过权限，任何库拒绝访问都必须令整轮失败。 */
    @Test
    void preservesAccessDeniedFailure() {
        ApiRuntimeException denied = new ApiRuntimeException("无知识库访问权限");
        doThrow(denied).when(bases).requireBase(11L, false, false);
        installIdentity(1L);
        List<Long> ids = List.of(11L, 12L);
        assertThatThrownBy(() -> biz.recallEnabledList(ids, "问题")).isSameAs(denied);
        assertContext(1L);
    }

    /** 每个外层调用独立初始化身份；内部子线程的扩展属性必须与父线程隔离。 */
    private Void recallForTenant(Long tenantId) {
        installIdentity(tenantId);
        try {
            List<Long> ids = List.of(tenantId * 10 + 1, tenantId * 10 + 2);
            List<List<KnowledgeRecallHit>> results = biz.recallEnabledList(ids, "问题");
            assertThat(results).hasSize(2);
            assertContext(tenantId);
            assertThat(RequestContext.getAttribute("child-base")).isNull();
            return null;
        } finally {
            clear();
        }
    }

    /** 创建合成测试身份，验证凭证仅在内存中跨线程使用。 */
    private void installIdentity(Long tenantId) {
        UserDetails user = new UserDetails();
        user.setId(tenantId + 100);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(tenantId);
        RequestContext.setToken("synthetic-token-" + tenantId);
        RequestContext.setTraceId("trace-" + tenantId);
        RequestContext.setAttribute("x-business", "business-" + tenantId);
        MDC.put("traceId", "trace-" + tenantId);
        List<GrantedAuthority> authorities = List.of();
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user-" + tenantId, "synthetic-credential", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 核对数据库隔离、下游请求头及Spring Security依赖的完整信息。 */
    private void assertContext(Long tenantId) {
        assertThat(RequestContext.getTenantCode()).isEqualTo(tenantId);
        assertThat(RequestContext.getUser().getId()).isEqualTo(tenantId + 100);
        assertThat(RequestContext.getToken()).isEqualTo("synthetic-token-" + tenantId);
        assertThat(RequestContext.getTraceId()).isEqualTo("trace-" + tenantId);
        assertThat(RequestContext.getAttribute("x-business")).isEqualTo("business-" + tenantId);
        assertThat(MDC.get("traceId")).isEqualTo("trace-" + tenantId);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("user-" + tenantId);
    }
}
