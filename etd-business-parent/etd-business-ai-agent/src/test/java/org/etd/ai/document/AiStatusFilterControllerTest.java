package org.etd.ai.document;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.etd.ai.document.biz.KnowledgeDocumentBizService;
import org.etd.ai.document.controller.KnowledgeDocumentController;
import org.etd.ai.document.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.biz.KnowledgeBizService;
import org.etd.ai.knowledge.controller.KnowledgeBaseController;
import org.etd.ai.knowledge.controller.KnowledgeFolderController;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.model.biz.ModelBizService;
import org.etd.ai.model.controller.ModelConfigController;
import org.etd.ai.model.controller.ModelConnectionController;
import org.etd.ai.model.service.ModelConfigService;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.framework.starter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证五类HTTP列表状态参数的可选绑定、合法值及服务端边界校验。 */
class AiStatusFilterControllerTest {
    /** 真实MVC参数绑定及全局异常处理入口。 */
    private MockMvc mvc;
    /** 方法参数校验器资源。 */
    private ValidatorFactory validators;
    /** 连接列表能力替身。 */
    private ModelConnectionService connections;
    /** 模型列表能力替身。 */
    private ModelConfigService models;
    /** 知识库列表能力替身。 */
    private KnowledgeBaseService bases;
    /** 目录列表能力替身。 */
    private KnowledgeFolderService folders;
    /** 文档列表能力替身。 */
    private KnowledgeDocumentService documents;

    /** 使用真实Controller、方法校验代理与全局异常处理，隔离数据库及外部服务。 */
    @BeforeEach
    void createMvc() {
        validators = Validation.buildDefaultValidatorFactory();
        connections = mock(ModelConnectionService.class);
        models = mock(ModelConfigService.class);
        bases = mock(KnowledgeBaseService.class);
        folders = mock(KnowledgeFolderService.class);
        documents = mock(KnowledgeDocumentService.class);
        stubListResponses();
        var modelBiz = mock(ModelBizService.class);
        var knowledgeBiz = mock(KnowledgeBizService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                validate(new ModelConnectionController(connections, modelBiz)),
                validate(new ModelConfigController(models, modelBiz)),
                validate(new KnowledgeBaseController(bases, knowledgeBiz)),
                validate(new KnowledgeFolderController(folders, bases, knowledgeBiz)),
                validate(new KnowledgeDocumentController(documents, mock(KnowledgeDocumentBizService.class), bases)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /** 释放校验器资源，避免测试之间共享状态。 */
    @AfterEach
    void closeValidator() {
        validators.close();
    }

    /** 不传状态时必须把null传给服务，不能自动改成启用。 */
    @Test
    void shouldTreatOmittedStatusAsAll() throws Exception {
        requestAllLists(null);
        verifyFilterForwarding(null);
    }

    /** 0与1都应原样传递，特别是不能把0误判为空参数。 */
    @Test
    void shouldForwardBothStatusValues() throws Exception {
        for (int value : List.of(0, 1)) {
            clearInvocations(connections, models, bases, folders, documents);
            requestAllLists(value);
            verifyFilterForwarding(value);
        }
    }

    /** 超出全局启停状态范围的参数由后端返回HTTP400。 */
    @Test
    void shouldRejectInvalidStatusValues() throws Exception {
        for (String path : listPaths()) {
            for (int value : List.of(-1, 2)) {
                mvc.perform(get(path).param("dataStatus", Integer.toString(value)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    /** 为所有管理列表提供空结果，测试只关注实际参数绑定及校验。 */
    private void stubListResponses() {
        when(connections.fetchPage(anyLong(), anyLong(), nullable(Integer.class))).thenReturn(new Page<>());
        when(models.fetchPage(anyLong(), anyLong(), nullable(String.class), nullable(Integer.class))).thenReturn(new Page<>());
        when(bases.fetchPage(anyLong(), anyLong(), nullable(String.class), nullable(Integer.class))).thenReturn(new Page<>());
        when(folders.fetchFolderList(anyLong(), nullable(Integer.class))).thenReturn(List.of());
        when(documents.fetchPage(anyLong(), nullable(Long.class), anyBoolean(), nullable(String.class),
                anyLong(), anyLong(), nullable(Integer.class))).thenReturn(new Page<>());
    }

    /** 通过真实方法校验拦截器执行Controller上的@Min/@Max约束。 */
    private Object validate(Object controller) {
        var factory = new ProxyFactory(controller);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new MethodValidationInterceptor(validators.getValidator()));
        return factory.getProxy();
    }

    /** 统一执行五类列表HTTP请求。 */
    private void requestAllLists(Integer value) throws Exception {
        for (String path : listPaths()) {
            MockHttpServletRequestBuilder request = get(path);
            if (value != null) {
                request.param("dataStatus", value.toString());
            }
            mvc.perform(request).andExpect(status().isOk());
        }
    }

    /** 确认真实Controller未丢弃或改写状态参数。 */
    private void verifyFilterForwarding(Integer value) {
        verify(connections).fetchPage(1, 10, value);
        verify(models).fetchPage(1, 10, null, value);
        verify(bases).fetchPage(1, 10, null, value);
        verify(folders).fetchFolderList(1L, value);
        verify(documents).fetchPage(1L, null, false, null, 1, 10, value);
    }

    /** @return 本轮增加状态筛选的五个真实接口路径 */
    private List<String> listPaths() {
        return List.of("/v1/model-connections", "/v1/model-configs", "/v1/knowledge-base",
                "/v1/knowledge-base/1/folders", "/v1/knowledge-base/1/documents");
    }
}
