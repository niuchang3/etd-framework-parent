package org.etd.ai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etd.framework.starter.client.autoconfigure.SecurityClientAutoConfiguration;
import com.etd.framework.starter.client.core.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.client.core.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.client.core.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.i18n.SecurityMessageResolver;
import com.etd.framework.starter.client.core.permission.PermissionChecker;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.Filter;

import org.etd.ai.document.controller.KnowledgeDocumentController;
import org.etd.ai.knowledge.controller.KnowledgeBaseController;
import org.etd.ai.knowledge.controller.KnowledgeFolderController;
import org.etd.ai.model.biz.ModelBizService;
import org.etd.ai.model.controller.ModelConfigController;
import org.etd.ai.model.controller.ModelConnectionController;
import org.etd.ai.model.entity.ModelConfigEntity;
import org.etd.ai.model.service.ModelConfigService;
import org.etd.ai.upload.controller.UploadController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 验证AI直接声明框架权限后，登录、功能read/write与公开健康路径的访问边界。 */
class AiEndpointAuthenticationTest {
    /** 仅装配真实安全过滤链、MVC及模型页面，外部认证存储使用替身。 */
    private AnnotationConfigWebApplicationContext context;

    /** 经过真实安全过滤链的HTTP测试入口。 */
    private MockMvc mvc;

    /** 创建隔离上下文，不连接数据库、Redis或模型服务。 */
    @BeforeEach
    void createSecurityContext() {
        context = new AnnotationConfigWebApplicationContext();
        var servletContext = new MockServletContext();
        context.setServletContext(servletContext);
        context.register(AuthenticationConfiguration.class);
        context.refresh();
        Filter securityFilter = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    /** 关闭过滤链上下文并清除线程认证，避免用例相互污染。 */
    @AfterEach
    void closeSecurityContext() {
        context.close();
        SecurityContextHolder.clearContext();
    }

    /** 匿名业务请求仍被框架认证入口拒绝。 */
    @Test
    void shouldRequireAuthenticationForBusinessEndpoints() throws Exception {
        var request = get("/v1/model-configs");
        mvc.perform(request).andExpect(status().isUnauthorized());
        var writeRequest = delete("/v1/model-configs/1");
        mvc.perform(writeRequest).andExpect(status().isUnauthorized());
    }

    /** 登录但无功能权限时，读写接口均拒绝且不能进入业务服务。 */
    @Test
    void shouldDenyAuthenticatedUserWithoutFunctionalAuthorities() throws Exception {
        var session = createSession();
        var readRequest = get("/v1/model-configs").session(session);
        mvc.perform(readRequest).andExpect(status().isForbidden());
        var writeRequest = delete("/v1/model-configs/1").session(session);
        mvc.perform(writeRequest).andExpect(status().isForbidden());
        var models = context.getBean(ModelConfigService.class);
        verifyNoInteractions(models);
    }

    /** read只允许查询，不能执行删除等写操作。 */
    @Test
    void shouldAllowReadWithoutGrantingWrite() throws Exception {
        var session = createSession("ai:model-config:read");
        var readRequest = get("/v1/model-configs").session(session);
        mvc.perform(readRequest).andExpect(status().isOk());
        var writeRequest = delete("/v1/model-configs/1").session(session);
        mvc.perform(writeRequest).andExpect(status().isForbidden());
        var models = context.getBean(ModelConfigService.class);
        verify(models).fetchPage(1L, 10L, null, null);
        verify(models, never()).removeModel(anyLong());
    }

    /** 同时具备read/write时，真实Controller可调用读写业务能力。 */
    @Test
    void shouldAllowGrantedReadAndWrite() throws Exception {
        var session = createSession("ai:model-config:read", "ai:model-config:write");
        var readRequest = get("/v1/model-configs").session(session);
        mvc.perform(readRequest).andExpect(status().isOk());
        var writeRequest = delete("/v1/model-configs/1").session(session);
        mvc.perform(writeRequest).andExpect(status().isOk());
        var models = context.getBean(ModelConfigService.class);
        verify(models).fetchPage(1L, 10L, null, null);
        verify(models).removeModel(1L);
    }

    /** 为请求装配指定authority，避免依赖外部UPMS令牌签发。 */
    private MockHttpSession createSession(String... authorityCodes) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String code : authorityCodes) {
            var authority = new SimpleGrantedAuthority(code);
            authorities.add(authority);
        }
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated("tenant-user", "", authorities);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        var session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    /** 健康路径仍使用框架匿名白名单，未将全部AI路径加入白名单。 */
    @Test
    void shouldKeepHealthPathPublic() throws Exception {
        var request = get("/actuator/health");
        mvc.perform(request).andExpect(status().isOk());
    }

    /** 六个入口直接使用框架注解，原资源码与菜单和前端协议保持一致。 */
    @Test
    void shouldDeclareFrameworkPermissionOnAllAiControllers() {
        var resources =
                Map.of(
                        ModelConnectionController.class, "ai:model-connection",
                        ModelConfigController.class, "ai:model-config",
                        KnowledgeBaseController.class, "ai:knowledge-base",
                        KnowledgeFolderController.class, "ai:knowledge-folder",
                        KnowledgeDocumentController.class, "ai:knowledge-document",
                        UploadController.class, "ai:knowledge-document");
        for (var resource : resources.entrySet()) {
            var permission =
                    AnnotatedElementUtils.findMergedAnnotation(resource.getKey(), Permission.class);
            assertNotNull(permission);
            assertEquals(resource.getValue(), permission.value());
        }
    }

    /** 仅提供健康响应，用于检查生产过滤链的匿名路径匹配。 */
    @RestController
    static class HealthController {
        /** 返回健康占位响应，不涉及外部基础设施探测。 */
        @GetMapping("/actuator/health")
        Map<String, String> getHealth() {
            return Map.of("status", "UP");
        }
    }

    /** 复用生产过滤链定义，认证依赖隔离，避免测试复制另一套放行策略。 */
    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    @EnableWebMvc
    static class AuthenticationConfiguration {
        /** 使用应用相同的健康白名单和authenticated兜底规则。 */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json)
                throws Exception {
            var properties = new SecurityProperties();
            var publicPaths = List.of("/actuator/health");
            properties.getPermissions().setIgnore(publicPaths);
            SecurityMessageResolver messages = (code, args, locale, defaultMessage) -> code;
            var denied = new AccessDeniedHandlerImpl(json, messages);
            var unauthenticated = new AuthenticationEntryPointImpl(json, messages);
            var configuration = new SecurityClientAutoConfiguration();
            return configuration.bearerAuthenticationSecurityFilterChain(
                    http, properties, denied, unauthenticated);
        }

        /** 使用生产框架方法授权检查器，不在测试中模拟权限校验结果。 */
        @Bean
        PermissionChecker permissionChecker() {
            return new PermissionChecker();
        }

        /** 本测试通过会话提供已认证身份；若意外触发解码则立即失败。 */
        @Bean
        TokenDecode<SignedJWT> tokenDecode() {
            return token -> {
                throw new IllegalStateException("本用例不应触发外部令牌解码");
            };
        }

        /** 隔离登录存储，测试不依赖Redis。 */
        @Bean
        UserLoginTokenStorage userLoginTokenStorage() {
            return mock(UserLoginTokenStorage.class);
        }

        /** 无效令牌分支的依赖替身，不影响匿名请求的真实401入口。 */
        @Bean
        EtdAuthenticationFailureHandler failureHandler() {
            return mock(EtdAuthenticationFailureHandler.class);
        }

        /** 供真实框架认证链与MVC响应序列化使用。 */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /** 不访问数据库，仅验证接口是否经过认证后进入服务。 */
        @Bean
        ModelConfigService modelConfigService() {
            var models = mock(ModelConfigService.class);
            var page = new Page<ModelConfigEntity>(1L, 10L);
            when(models.fetchPage(1L, 10L, null, null)).thenReturn(page);
            return models;
        }

        /** 注册实际AI模型Controller，保留方法安全代理参与调用。 */
        @Bean
        ModelConfigController modelConfigController(ModelConfigService models) {
            var biz = mock(ModelBizService.class);
            return new ModelConfigController(models, biz);
        }

        /** 提供白名单匹配的公开端点。 */
        @Bean
        HealthController healthController() {
            return new HealthController();
        }
    }
}
