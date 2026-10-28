package org.etd.framework.starter.web.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolationException;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.spring.SpringContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 在非AI模块验证全局异常分派，确保开发环境的响应及日志均不回显敏感数据。 */
class GlobalExceptionHandlerTest {
    /** 模拟敏感输入和底层SQL，不能出现在日志或响应中。 */
    private static final String SECRET = "test-only-sensitive-input";
    /** 当前用例的MVC入口，不启动任何业务应用。 */
    private MockMvc mvc;
    /** 独立开发环境上下文，用于触发ResultModel潜在的开发堆栈分支。 */
    private GenericApplicationContext context;
    /** 只在单个测试作用域替换静态上下文访问，不污染其他测试。 */
    private MockedStatic<SpringContextHelper> contexts;
    /** 捕获全局处理器日志，验证没有异常堆栈或原始值。 */
    private ListAppender<ILoggingEvent> logs;
    /** 仅捕获当前处理器，避免依赖整个测试进程的日志配置。 */
    private Logger logger;

    /** 创建开发环境和MVC，所有异常交由真实全局处理器选择对应方法。 */
    @BeforeEach
    void createMvc() {
        context = new GenericApplicationContext();
        context.getEnvironment().setActiveProfiles("dev");
        context.refresh();
        contexts = mockStatic(SpringContextHelper.class);
        contexts.when(SpringContextHelper::getApplicationContext).thenReturn(context);
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /** 恢复静态上下文并释放日志捕获，其他模块测试保持原有环境。 */
    @AfterEach
    void closeResources() {
        contexts.close();
        context.close();
        logger.detachAppender(logs);
        logs.stop();
    }

    /** 异常真实携带敏感值时，HTTP状态、业务码及脱敏策略仍需正确。 */
    @ParameterizedTest
    @CsvSource({"/validation,400,400", "/constraint,400,400", "/json,400,400",
            "/duplicate,200,5001", "/integrity,200,5001"})
    void shouldRedactRequestAndDatabaseErrors(String path, int httpStatus, int code) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        if ("/json".equals(path)) {
            request = post(path).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"count\":\"" + SECRET + "\"}");
        }
        String body = mvc.perform(request).andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.devMessage").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(SECRET, "INSERT INTO", "FOREIGN KEY");
        assertThat(logs.list).isNotEmpty();
        for (ILoggingEvent event : logs.list) {
            assertThat(event.getFormattedMessage()).doesNotContain(SECRET, "INSERT INTO", "FOREIGN KEY");
            assertThat(event.getThrowableProxy()).isNull();
        }
    }

    /** 数据库子类必须由更具体的唯一约束处理器接管，不能落入完整性或500处理器。 */
    @Test
    void shouldSelectSpecificDuplicateHandler() throws Exception {
        mvc.perform(get("/duplicate")).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("数据已存在，请检查唯一性约束"));
    }

    /** 业务语义继续由ApiRuntimeException维护，不被数据库通用提示覆盖。 */
    @Test
    void shouldPreserveBusinessError() throws Exception {
        mvc.perform(get("/business")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5001))
                .andExpect(jsonPath("$.message").value("目录非空，不能删除"));
    }

    /** 安全异常继续传播给Security过滤器，不能被合并后的处理器转换成普通业务失败。 */
    @Test
    void shouldPropagateSecurityErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        AccessDeniedException denied = new AccessDeniedException("denied");
        BadCredentialsException invalid = new BadCredentialsException("invalid");
        assertThatThrownBy(() -> handler.propagateSecurityException(denied)).isSameAs(denied);
        assertThatThrownBy(() -> handler.propagateSecurityException(invalid)).isSameAs(invalid);
    }

    /** 仅用于触发Jackson原始值解析异常的请求模型。 */
    public record JsonRequest(/** 必须是整数，字符串输入应触发解析错误。 */ int count) { }

    /** 探针控制器位于框架测试包，证明异常处理没有AI包名限制。 */
    @RestController
    static class ProbeController {
        /** 构造包含被拒绝原值和自定义敏感消息的Body校验异常。 */
        @GetMapping("/validation")
        public void rejectBody() throws Exception {
            BeanPropertyBindingResult result = new BeanPropertyBindingResult(new JsonRequest(0), "request");
            result.addError(new FieldError("request", "apiKey", SECRET, false,
                    null, null, SECRET));
            Method method = ProbeController.class.getMethod("acceptJson", JsonRequest.class);
            throw new MethodArgumentNotValidException(new MethodParameter(method, 0), result);
        }

        /** 自定义约束消息可以包含原始值，处理器不能输出该消息。 */
        @GetMapping("/constraint")
        public void rejectConstraint() {
            throw new ConstraintViolationException(SECRET, Set.of());
        }

        /** 使用真实Jackson反序列化，失败消息可能包含原始凭证片段。 */
        @PostMapping("/json")
        public void acceptJson(@RequestBody JsonRequest request) { }

        /** 模拟底层唯一约束异常携带SQL及字段值。 */
        @GetMapping("/duplicate")
        public void rejectDuplicate() {
            throw new DuplicateKeyException("INSERT INTO credentials VALUES ('" + SECRET + "')");
        }

        /** 模拟底层外键异常，通用提示不应公开内部结构。 */
        @GetMapping("/integrity")
        public void rejectIntegrity() {
            throw new DataIntegrityViolationException("FOREIGN KEY credentials " + SECRET);
        }

        /** 业务错误保留清晰的领域提示。 */
        @GetMapping("/business")
        public void rejectBusiness() {
            throw new ApiRuntimeException("目录非空，不能删除");
        }
    }
}
