package org.etd.ai.model;

import org.etd.ai.model.biz.ModelSquareBizService;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.ai.model.service.ModelSquareService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Embedding连接身份稳定性，凭证轮换不会被误判为向量空间变化。 */
class ModelEmbeddingIdentityTest {
    /** 地址或协议改变必须使用新连接身份；旧ID不能继续命中旧索引。 */
    @Test
    void rejectsEndpointChangeButAllowsCredentialRotation() {
        ModelService models = mock(ModelService.class);
        ModelConnectionService connections = mock(ModelConnectionService.class);
        ModelSquareService queries = mock(ModelSquareService.class);
        ModelSquareBizService biz = new ModelSquareBizService(queries, models, connections);
        ModelEntity model = new ModelEntity();
        model.setModelType(ModelType.EMBEDDING);
        when(models.requireModel(10L)).thenReturn(model);
        ModelConnectionEntity old = connection("https://example.invalid/old");
        when(connections.requireConnection(100L, true)).thenReturn(old);
        ModelConnectionEntity changed = connection("https://example.invalid/new");
        assertThatThrownBy(() -> biz.modifyConnection(changed, null, 0L)).hasMessageContaining("重建知识库索引");
        verify(connections, never()).modifyConnection(any(), any(), any());
        ModelConnectionEntity rotated = connection("https://example.invalid/old");
        biz.modifyConnection(rotated, "synthetic-rotated-key", 0L);
        verify(connections).modifyConnection(rotated, "synthetic-rotated-key", 0L);
    }

    private ModelConnectionEntity connection(String url) {
        ModelConnectionEntity entity = new ModelConnectionEntity();
        entity.setId(100L);
        entity.setModelId(10L);
        entity.setBaseUrl(url);
        entity.setProtocol("openai_compatible");
        return entity;
    }
}
