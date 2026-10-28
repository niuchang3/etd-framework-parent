package org.etd.framework.starter.web;

import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.web.interceptor.extend.EtdFrameworkHttpRequestInterceptorImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 通过真实MVC异步派发验证SSE交出线程后立即清理租户与Token，防止线程池复用污染。 */
class WebAsyncContextCleanupTest {
    /** 移除测试线程残余。 */
    @AfterEach
    void clear() {
        RequestContext.clean();
    }

    /** 流尚未结束时，原Servlet线程已经清理；最终派发也不遗留上下文。 */
    @Test
    void clearsOriginalServletThreadBeforeStreamCompletes() throws Exception {
        SseEmitter emitter = new SseEmitter();
        StreamingController controller = new StreamingController(emitter);
        EtdFrameworkHttpRequestInterceptorImpl interceptor = new EtdFrameworkHttpRequestInterceptorImpl();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(interceptor).build();
        MockHttpServletRequestBuilder request = get("/stream-test").header("tenant-code", "1").header("Authorization", "synthetic-token");
        MvcResult result = mvc.perform(request).andExpect(request().asyncStarted()).andReturn();
        assertThat(RequestContext.getTenantCode()).isNull();
        assertThat(RequestContext.getToken()).isNull();
        emitter.complete();
        RequestBuilder dispatch = asyncDispatch(result);
        mvc.perform(dispatch).andExpect(status().isOk());
        assertThat(RequestContext.getTenantCode()).isNull();
        assertThat(RequestContext.getToken()).isNull();
    }

    /** 仅用于触发真实SSE生命周期的测试入口。 */
    @RestController
    static class StreamingController {
        private final SseEmitter emitter;

        StreamingController(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /** @return 测试线程可控制完成时点的异步流 */
        @GetMapping("/stream-test")
        public SseEmitter stream() {
            assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
            return emitter;
        }
    }
}
