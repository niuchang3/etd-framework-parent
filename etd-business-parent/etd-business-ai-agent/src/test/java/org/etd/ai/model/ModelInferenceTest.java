package org.etd.ai.model;

import java.util.ArrayList;
import java.util.List;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 验证业务向量调用的批量边界、服务商响应顺序以及错误脱敏。 */
class ModelInferenceTest {
    private ModelInferenceService inference;
    private EmbeddingModel model;

    /** 模拟原生模型接口，保留真实请求组装与响应校验。 */
    @BeforeEach
    void prepare() {
        ModelConnectionMapper connections = mock(ModelConnectionMapper.class);
        ModelClientRegistry clients = mock(ModelClientRegistry.class);
        model = mock(EmbeddingModel.class);
        ModelConnectionEntity connection = new ModelConnectionEntity();
        connection.setTenantId(900001L);
        when(connections.selectById(30L)).thenReturn(connection);
        when(clients.getEmbeddingModel("900001", "30")).thenReturn(model);
        inference = new ModelInferenceService(connections, clients);
    }

    /** 按服务商index重排批量结果，第二批的局部index重新从0开始。 */
    @Test
    void restoresInputOrderAcrossBatches() {
        when(model.call(any(EmbeddingRequest.class))).thenAnswer(call -> {
            EmbeddingRequest request = call.getArgument(0);
            assertThat(request.getOptions().getDimensions()).isEqualTo(2);
            assertThat(request.getInstructions()).hasSizeLessThanOrEqualTo(16);
            List<Embedding> results = new ArrayList<>();
            for (int index = request.getInstructions().size() - 1; index >= 0; index--) {
                float[] vector = {Float.parseFloat(request.getInstructions().get(index)) + 1, 1};
                Embedding embedding = new Embedding(vector, index);
                results.add(embedding);
            }
            return new EmbeddingResponse(results);
        });
        List<String> texts = new ArrayList<>();
        for (int index = 0; index < 17; index++) {
            texts.add(Integer.toString(index));
        }
        List<float[]> result = inference.createEmbeddingList(30L, 2, texts);
        assertThat(result).hasSize(17);
        for (int index = 0; index < result.size(); index++) {
            assertThat(result.get(index)[0]).isEqualTo(index + 1);
        }
        verify(model, times(2)).call(any(EmbeddingRequest.class));
    }

    /** 缺项、错误维度、零向量和非有限数值均不可写入余弦向量空间。 */
    @Test
    void rejectsMalformedVectorsWithoutLeakingProviderDetails() {
        List<String> texts = List.of("问题");
        float[] wrongDimensions = {1};
        float[] zero = {0, 0};
        float[] nonFinite = {Float.NaN, 1};
        List<float[]> invalid = List.of(wrongDimensions, zero, nonFinite);
        for (float[] vector : invalid) {
            Embedding embedding = new Embedding(vector, 0);
            List<Embedding> results = List.of(embedding);
            EmbeddingResponse response = new EmbeddingResponse(results);
            when(model.call(any(EmbeddingRequest.class))).thenReturn(response);
            assertThatThrownBy(() -> inference.createEmbeddingList(30L, 2, texts))
                    .isInstanceOf(ApiRuntimeException.class).hasMessageContaining("向量生成失败");
        }
        List<Embedding> emptyResults = List.of();
        EmbeddingResponse empty = new EmbeddingResponse(emptyResults);
        when(model.call(any(EmbeddingRequest.class))).thenReturn(empty);
        assertThatThrownBy(() -> inference.createEmbeddingList(30L, 2, texts)).hasMessageContaining("向量生成失败");
        when(model.call(any(EmbeddingRequest.class))).thenThrow(new IllegalStateException("sensitive-provider-body"));
        assertThatThrownBy(() -> inference.createEmbeddingList(30L, 2, texts))
                .hasMessageNotContaining("sensitive-provider-body");
    }
}
