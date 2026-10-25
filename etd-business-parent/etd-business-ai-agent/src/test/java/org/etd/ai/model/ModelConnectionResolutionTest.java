package org.etd.ai.model;

import org.etd.ai.model.component.BusinessModelConnectionResolver;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.ai.model.constant.ModelProtocol;
import org.etd.ai.model.constant.ModelType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** 验证业务模型解析的归属校验、配置版本及凭证惰性解密边界。 */
class ModelConnectionResolutionTest {
    /** 模型目录和租户连接任一更新都产生不同缓存版本，无认证连接不触发解密。 */
    @Test
    void resolvesBothVersionsAndChecksTenantOwnership() {
        var connections = mock(ModelConnectionMapper.class);
        var models = mock(ModelMapper.class);
        var cipher = mock(ModelCredentialCipher.class);
        var resolver = new BusinessModelConnectionResolver(connections, models, cipher);
        var connection = createConnection();
        var model = createModel();
        when(connections.selectById(30L)).thenReturn(connection);
        when(models.selectById(20L)).thenReturn(model);
        var original = resolver.resolveAuthorized("900001", "30");
        assertThat(original.resolveApiKey()).isNull();
        verify(connections).selectById(30L);
        verifyNoInteractions(cipher);
        model.setVersion(1L);
        var changedModel = resolver.resolveAuthorized("900001", "30");
        assertThat(changedModel.version()).isNotEqualTo(original.version());
        connection.setVersion(1L);
        var changedConnection = resolver.resolveAuthorized("900001", "30");
        assertThat(changedConnection.version()).isNotEqualTo(changedModel.version());
        assertThatThrownBy(() -> resolver.resolveAuthorized("900002", "30")).hasMessageContaining("不存在或已停用");
        model.setDataStatus(BasicConstant.DataStatus.DISABLED_CODE);
        assertThatThrownBy(() -> resolver.resolveAuthorized("900001", "30")).hasMessageContaining("模型不存在或已停用");
    }

    /** 连接实体只携带测试地址，无凭证模式可验证完整解析而无需密钥。 */
    private ModelConnectionEntity createConnection() {
        var connection = new ModelConnectionEntity();
        connection.setId(30L);
        connection.setModelId(20L);
        connection.setTenantId(900001L);
        connection.setVersion(0L);
        connection.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        connection.setProtocol(ModelClientProtocol.OPENAI_COMPATIBLE);
        connection.setAuthType(ModelProtocol.AUTH_NONE);
        connection.setBaseUrl("http://localhost/v1");
        return connection;
    }

    /** 只构造解析所需目录属性，能力代码复用现有定义。 */
    private ModelEntity createModel() {
        var model = new ModelEntity();
        model.setVersion(0L);
        model.setModelCode("test-embedding");
        model.setModelType(ModelType.EMBEDDING);
        model.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        return model;
    }
}
