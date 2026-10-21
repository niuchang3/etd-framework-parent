package org.etd.upms.security;

import com.etd.framework.starter.client.core.permission.PermissionChecker;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.servlet.Filter;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 在真实 MVC 方法授权中验证框架数组权限，使用应用已有测试依赖且不访问外部服务。 */
class PermissionArrayAuthorizationTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    /** 装配原生方法授权和框架权限检查器，确保验证注解解析及 OR 判断的完整链路。 */
    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        MockServletContext servletContext = new MockServletContext();
        context.setServletContext(servletContext);
        context.register(TestConfiguration.class);
        context.refresh();
        Filter securityFilter = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    /** 清理认证状态及 MVC 上下文，防止测试间串扰。 */
    @AfterEach
    void tearDown() {
        context.close();
        SecurityContextHolder.clearContext();
    }

    /** 类数组按任一权限放行，方法数组整体覆盖，空方法数组继承类，读写操作独立校验。 */
    @ParameterizedTest
    @CsvSource({
            "class, test:organization:read, 200",
            "class, test:user:read, 200",
            "class, test:other:read, 403",
            "class, test:user:write, 403",
            "inherited, test:user:read, 200",
            "override, test:role:read, 200",
            "override, test:config:read, 200",
            "override, test:organization:read, 403",
            "write, test:user:read, 403",
            "write, test:user:write, 200"
    })
    void shouldAuthorizeByResourceArray(String endpoint, String authority, int expectedStatus) throws Exception {
        UserDetails user = new UserDetails();
        MockHttpSession session = createSession(authority, user);
        var request = get("/test-permission/" + endpoint).session(session);
        mvc.perform(request).andExpect(status().is(expectedStatus));
    }

    /** 数组权限不改变认证要求。 */
    @Test
    void shouldRejectAnonymousRequest() throws Exception {
        var request = get("/test-permission/class");
        mvc.perform(request).andExpect(status().isUnauthorized());
    }

    /** 平台管理员可绕过菜单授权，但锁定后仍不能执行写操作。 */
    @Test
    void shouldPreservePlatformAdminAndReadOnlyRules() throws Exception {
        UserDetails user = new UserDetails();
        user.setPlatformAdmin(true);
        MockHttpSession session = createSession("test:other:read", user);
        var read = get("/test-permission/class").session(session);
        mvc.perform(read).andExpect(status().isOk());
        var write = get("/test-permission/write").session(session);
        mvc.perform(write).andExpect(status().isOk());
        user.setLocked(true);
        var lockedWrite = get("/test-permission/write").session(session);
        mvc.perform(lockedWrite).andExpect(status().isForbidden());
    }

    /** 即使首项权限已满足，后续非法码也必须报错；最终空数组同样不能放行。 */
    @ParameterizedTest
    @CsvSource({"invalid", "blank", "empty"})
    void shouldRejectInvalidConfiguration(String endpoint) {
        UserDetails user = new UserDetails();
        MockHttpSession session = createSession("test:user:read", user);
        var request = get("/test-permission/" + endpoint).session(session);
        Exception exception = assertThrows(Exception.class, () -> mvc.perform(request));
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertInstanceOf(IllegalArgumentException.class, cause);
    }

    /** 构造只有一个授权的登录身份，用于证明无需同时满足整个数组。 */
    private MockHttpSession createSession(String authority, UserDetails user) {
        SimpleGrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority);
        List<SimpleGrantedAuthority> authorities = List.of(grantedAuthority);
        var authentication = UsernamePasswordAuthenticationToken.authenticated("test-user", null, authorities);
        authentication.setDetails(user);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    /** 测试仅注册框架授权能力及专用端点。 */
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfiguration {

        /** 所有测试端点必须先通过登录校验。 */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint((request, response, exception) -> response.setStatus(401)))
                    .build();
        }

        /** 使用生产实现解析资源数组及操作类型。 */
        @Bean
        PermissionChecker permissionChecker() {
            return new PermissionChecker();
        }

        /** 装配类权限及方法覆盖场景。 */
        @Bean
        ArrayController arrayController() {
            return new ArrayController();
        }

        /** 装配没有类资源可继承的错误配置场景。 */
        @Bean
        EmptyController emptyController() {
            return new EmptyController();
        }
    }

    /** 模拟多个菜单共享接口及方法级配置，不承载业务逻辑。 */
    @RestController
    @RequestMapping("/test-permission")
    @Permission({"test:organization", "test:user"})
    static class ArrayController {

        /** 直接使用类上的资源数组，GET 自动要求读取权限。 */
        @GetMapping("/class")
        public String getClassPermission() {
            return "ok";
        }

        /** 方法仅指定操作，资源数组继续继承类声明。 */
        @Permission(action = PermissionAction.READ)
        @GetMapping("/inherited")
        public String getInheritedPermission() {
            return "ok";
        }

        /** 方法数组替换整个类数组。 */
        @Permission({"test:role", "test:config"})
        @GetMapping("/override")
        public String getOverridePermission() {
            return "ok";
        }

        /** 显式写操作优先于 HTTP GET 的自动推导。 */
        @Permission(action = PermissionAction.WRITE)
        @GetMapping("/write")
        public String getWritePermission() {
            return "ok";
        }

        /** 非法元素位于可匹配元素后，验证不能因短路而遗漏配置检查。 */
        @Permission({"test:user", "INVALID"})
        @GetMapping("/invalid")
        public String getInvalidPermission() {
            return "ok";
        }

        /** 数组中的空字符串不能作为继承声明。 */
        @Permission({"test:user", ""})
        @GetMapping("/blank")
        public String getBlankPermission() {
            return "ok";
        }
    }

    /** 没有类声明时，空数组属于无效配置。 */
    @RestController
    static class EmptyController {

        /** 不提供有效资源时必须报错，不能按无需权限处理。 */
        @Permission
        @GetMapping("/test-permission/empty")
        public String getEmptyPermission() {
            return "ok";
        }
    }
}
