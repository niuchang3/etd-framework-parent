package org.etd.framework.starter.ai.runtime;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.ai.runtime.context.RequestContextFlux;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 在复用同一实际线程的前提下，校验正常、异常及取消后的身份、安全上下文和 MDC 恢复。 */
class RequestContextFluxTest {
    @AfterEach
    void clean() {
        RequestContext.clean();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"complete", "error", "cancel"})
    void restoresOriginalWorkerContextAcrossEveryExit(String mode) throws Exception {
        identity(1L);
        RequestContextSnapshot snapshot = RequestContextSnapshot.capture();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> seed = executor.submit(() -> identity(99L));
            seed.get();
            Scheduler scheduler = Schedulers.fromExecutorService(executor);
            try {
                Flux<String> source = Flux.defer(() -> source(mode)).doOnRequest(this::assertRequestIdentity)
                        .doOnCancel(this::assertRunIdentity).publishOn(scheduler);
                RequestContextFlux<String> scoped = new RequestContextFlux<>(source, snapshot);
                Flux<String> verified = scoped.doOnNext(this::assertSignalIdentity);
                if ("cancel".equals(mode)) {
                    verified = verified.take(1);
                }
                Flux<String> result = verified;
                if ("error".equals(mode)) {
                    assertThatThrownBy(() -> result.collectList().block(Duration.ofSeconds(3)))
                            .isInstanceOf(IllegalStateException.class).hasMessage("受控失败");
                } else {
                    assertThat(result.collectList().block(Duration.ofSeconds(3))).containsExactly("增量");
                }
                Future<?> restored = executor.submit(() -> assertIdentity(99L));
                restored.get();
                assertIdentity(1L);
            } finally {
                scheduler.dispose();
            }
        }
    }

    private Flux<String> source(String mode) {
        assertRunIdentity();
        Flux<String> first = Flux.just("增量");
        if ("error".equals(mode)) {
            IllegalStateException failure = new IllegalStateException("受控失败");
            Flux<String> error = Flux.error(failure);
            return Flux.concat(first, error);
        }
        if ("cancel".equals(mode)) {
            Flux<String> pending = Flux.never();
            return Flux.concat(first, pending);
        }
        return first;
    }

    private void assertRequestIdentity(long requested) {
        assertRunIdentity();
    }

    private void assertSignalIdentity(String text) {
        assertRunIdentity();
    }

    private void assertRunIdentity() {
        assertIdentity(1L);
    }

    private void identity(Long tenant) {
        UserDetails user = new UserDetails();
        user.setId(tenant);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(tenant);
        RequestContext.setToken("synthetic-" + tenant);
        RequestContext.setAttribute("x-business", "business-" + tenant);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated("user-" + tenant, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MDC.put("trace", "trace-" + tenant);
    }

    private void assertIdentity(Long tenant) {
        assertThat(RequestContext.getTenantCode()).isEqualTo(tenant);
        assertThat(RequestContext.getUser().getId()).isEqualTo(tenant);
        assertThat(RequestContext.getToken()).isEqualTo("synthetic-" + tenant);
        assertThat(RequestContext.getHeaderContext().toMap()).containsEntry("x-business", "business-" + tenant);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user-" + tenant);
        assertThat(MDC.get("trace")).isEqualTo("trace-" + tenant);
    }
}
