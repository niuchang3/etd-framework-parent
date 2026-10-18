package org.etd.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.ai.model.controller.dto.ModelConnectionSaveDTO;
import org.etd.ai.model.converter.ModelConfigConverter;
import org.etd.ai.model.converter.ModelConnectionConverter;
import org.etd.ai.model.entity.ModelConfigEntity;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证生成的转换实现保持凭证边界、名称规范化及页面标记语义。 */
class ModelConversionTest {
    /** 响应仅提供凭证是否存在，不泄露密文及内部密钥标识。 */
    @Test
    void shouldExposeCredentialStatusWithoutSecrets() throws Exception {
        var entity = new ModelConnectionEntity();
        entity.setConnectionName("测试连接");
        entity.setApiKeyCiphertext("encrypted-test-value");
        entity.setCredentialKeyId("test-key-id");
        var response = ModelConnectionConverter.INSTANCE.toVO(entity);
        assertTrue(response.getCredentialConfigured());
        var json = new ObjectMapper();
        String serialized = json.writeValueAsString(response);
        assertFalse(serialized.contains("encrypted-test-value"));
        assertFalse(serialized.contains("test-key-id"));
        assertEquals("测试连接", response.getConnectionName());
        entity.setApiKeyCiphertext(null);
        assertFalse(ModelConnectionConverter.INSTANCE.toVO(entity).getCredentialConfigured());
    }

    /** 请求规范化名称，明文凭证由服务独立加密，可信归属不由转换器赋值。 */
    @Test
    void shouldKeepCredentialAndOwnershipOutsideRequestMapping() {
        var dto = new ModelConnectionSaveDTO();
        dto.setConnectionName("  测试连接  ");
        dto.setApiKey("test-only-input");
        var entity = ModelConnectionConverter.INSTANCE.toEntity(dto);
        assertEquals("测试连接", entity.getConnectionName());
        assertNull(entity.getApiKeyCiphertext());
        assertNull(entity.getCredentialKeyId());
        assertNull(entity.getTenantId());
        assertNull(entity.getCreateBy());
        var model = new ModelConfigEntity();
        model.setEmbeddingDimensions(1024);
        var response = ModelConfigConverter.INSTANCE.toVO(model);
        assertEquals(1024, response.getEmbeddingDimensions());
        assertFalse(response.getVerified());
    }
}
