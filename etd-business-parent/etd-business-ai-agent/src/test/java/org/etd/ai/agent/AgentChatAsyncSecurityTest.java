package org.etd.ai.agent;

import com.etd.framework.starter.client.core.authentication.bearer.BearerAuthenticationConverter;
import com.etd.framework.starter.client.core.authentication.bearer.BearerTokenAuthenticationFilter;
import com.etd.framework.starter.client.core.permission.PermissionChecker;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.servlet.Filter;
import java.io.IOException;
import java.util.List;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实安全过滤链验证Bearer认证在SSE异步派发中保持有效，且不放宽匿名访问或菜单权限。 */
class AgentChatAsyncSecurityTest {
    /** 仅装配MVC及安全链，避免测试依赖真实数据库、Redis及模型。 */
    private AnnotationConfigWebApplicationContext context;
    /** 同时经过安全过滤链和MVC的请求入口。 */
    private MockMvc mvc;

    /** 使用生产Bearer过滤器，认证管理器只处理合成测试令牌。 */
    @BeforeEach
    void initialize() {
        context = new AnnotationConfigWebApplicationContext();
        MockServletContext servletContext = new MockServletContext();
        context.setServletContext(servletContext);
        context.register(TestConfiguration.class);
        context.refresh();
        Filter security = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    /** 清理线程安全上下文及测试容器。 */
    @AfterEach
    void close() {
        context.close();
        SecurityContextHolder.clearContext();
    }

    /** 首次认证仅执行一次，原线程清理后，ASYNC派发仍能授权并完整结束SSE。 */
    @Test
    void preservesBearerAuthenticationDuringAsyncDispatch() throws Exception {
        MockHttpServletRequestBuilder request = streamRequest("allowed");
        MvcResult result = mvc.perform(request).andExpect(status().isOk())
                .andExpect(request().asyncStarted()).andReturn();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(result.getRequest().getSession(false)).isNull();
        result.getAsyncResult(3000);
        RequestBuilder dispatch = asyncDispatch(result);
        mvc.perform(dispatch).andExpect(status().isOk()).andExpect(content().string(containsString("test-answer")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        AuthenticationManager manager = context.getBean(AuthenticationManager.class);
        verify(manager, times(1)).authenticate(any());
        // 新的HTTP请求不能继承上一轮请求属性中的身份，即使前一轮认证已通过。
        MockHttpServletRequestBuilder anonymous = post("/agent/chat/sessions/1/stream");
        mvc.perform(anonymous).andExpect(status().isUnauthorized());
    }

    /** 同步接口也继续使用相同的Bearer认证入口。 */
    @Test
    void authenticatesNormalRequestAndRejectsMissingToken() throws Exception {
        MockHttpServletRequestBuilder allowed = get("/agent/chat/security-test").header(HttpHeaders.AUTHORIZATION, "Bearer allowed");
        mvc.perform(allowed).andExpect(status().isOk());
        MockHttpServletRequestBuilder anonymous = get("/agent/chat/security-test");
        mvc.perform(anonymous).andExpect(status().isUnauthorized());
    }

    /** 错误令牌不能启动流式请求。 */
    @Test
    void rejectsInvalidBearerToken() throws Exception {
        MockHttpServletRequestBuilder request = streamRequest("invalid");
        mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(request().asyncNotStarted());
    }

    /** 有效登录身份不代表有Agent菜单读权限，ASYNC支持不能绕过方法授权。 */
    @Test
    void rejectsAuthenticatedUserWithoutAgentPermission() throws Exception {
        MockHttpServletRequestBuilder request = streamRequest("no-permission");
        mvc.perform(request).andExpect(status().isForbidden()).andExpect(request().asyncNotStarted());
    }

    /** 公开健康路径保持匿名可访问。 */
    @Test
    void keepsHealthEndpointPublic() throws Exception {
        MockHttpServletRequestBuilder request = get("/actuator/health");
        mvc.perform(request).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder streamRequest(String token) {
        return post("/agent/chat/sessions/1/stream").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"问题\"}");
    }

    /** 同生产链一致：先恢复SecurityContext，再执行Bearer认证、匿名身份处理及HTTP和方法授权。 */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfiguration {
        /** @return 框架菜单权限检查器 */
        @Bean
        PermissionChecker permissionChecker() {
            return new PermissionChecker();
        }

        /** @return 可验证调用次数的合成令牌认证管理器 */
        @Bean
        AuthenticationManager authenticationManager() {
            AuthenticationManager manager = mock(AuthenticationManager.class);
            when(manager.authenticate(any())).thenAnswer(call -> authenticate(call.getArgument(0)));
            return manager;
        }

        private Authentication authenticate(Authentication input) {
            String token = input.getCredentials().toString();
            if ("invalid".equals(token)) {
                throw new BadCredentialsException("测试令牌无效");
            }
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ai:agent:read");
            List<SimpleGrantedAuthority> authorities = "allowed".equals(token) ? List.of(authority) : List.of();
            UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken.authenticated("test-user", null, authorities);
            UserDetails user = new UserDetails();
            user.setId(1L);
            result.setDetails(user);
            return result;
        }

        /** @param http 安全构建器 @param manager 合成认证 @return 包含生产Bearer过滤器的真实安全链 */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager manager) throws Exception {
            AuthenticationConverter converter = new BearerAuthenticationConverter();
            AuthenticationFailureHandler failure = (request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value());
            BearerTokenAuthenticationFilter bearer = new BearerTokenAuthenticationFilter(converter, manager, failure);
            http.csrf(AbstractHttpConfigurer::disable).requestCache(AbstractHttpConfigurer::disable)
                    .addFilterBefore(bearer, AnonymousAuthenticationFilter.class)
                    .authorizeHttpRequests(authorize -> authorize.requestMatchers("/actuator/health").permitAll().anyRequest().authenticated())
                    .exceptionHandling(errors -> errors
                            .authenticationEntryPoint((request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value()))
                            .accessDeniedHandler((request, response, exception) -> response.setStatus(HttpStatus.FORBIDDEN.value())));
            return http.build();
        }

        /** @return 只返回合成流事件的受保护测试入口 */
        @Bean
        ChatController chatController() {
            return new ChatController();
        }
    }

    /** 复现实际聊天接口的POST、SSE返回类型及Agent菜单权限。 */
    @RestController
    static class ChatController {
        /** @return 已完成的合成SSE流 @throws IOException 输出失败 */
        @PostMapping(value = "/agent/chat/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        @Permission(value = "ai:agent", action = PermissionAction.READ)
        @AutoLog("测试Agent异步认证")
        public SseEmitter stream() throws IOException {
            SseEmitter emitter = new SseEmitter(3000L);
            SseEmitter.SseEventBuilder event = SseEmitter.event().name("delta").data("test-answer");
            emitter.send(event);
            emitter.complete();
            return emitter;
        }

        /** @return 同步访问校验结果 */
        @GetMapping("/agent/chat/security-test")
        @Permission(value = "ai:agent", action = PermissionAction.READ)
        @AutoLog("测试Agent同步认证")
        public String get() {
            return "ok";
        }

        /** @return 公开健康检查结果 */
        @GetMapping("/actuator/health")
        @AutoLog("测试公开健康检查")
        public String health() {
            return "ok";
        }
    }
}
