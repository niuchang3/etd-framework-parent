package org.etd.ai.model.service;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;

/** 业务模型推理边界，复用框架SDK并统一阻止服务商错误正文进入业务响应。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelInferenceService {
    /** 单次最多提交16条正文，避免超过常用服务商的批量输入限制。 */
    private static final int EMBEDDING_BATCH_SIZE = 16;
    private final ModelConnectionMapper connections;
    private final ModelClientRegistry clients;

    /**
     * @param connectionId 经过业务配置校验的连接ID
     * @param dimensions 本次输出维度，必须与向量存储一致
     * @param texts 按业务顺序排列的正文
     * @return 与输入逐条对应的有限、非零向量
     * @throws ApiRuntimeException 连接不可访问、服务商失败或响应不完整
     */
    public List<float[]> createEmbeddingList(Long connectionId, Integer dimensions, List<String> texts) {
        try {
            String tenantId = requireTenantId(connectionId);
            String key = connectionId.toString();
            var model = clients.getEmbeddingModel(tenantId, key);
            var options = EmbeddingOptions.builder().dimensions(dimensions).build();
            List<float[]> vectors = new ArrayList<>();
            for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
                var batch = texts.subList(start, Math.min(start + EMBEDDING_BATCH_SIZE, texts.size()));
                var result = createEmbeddingBatch(model, batch, options, dimensions);
                vectors.addAll(result);
            }
            return vectors;
        } catch (RuntimeException exception) {
            log.warn("模型向量调用失败，连接ID={}，异常类型={}", connectionId, exception.getClass().getSimpleName());
            throw new ApiRuntimeException("向量生成失败，请检查模型连接、维度和服务状态后重试");
        }
    }

    /** 服务商可乱序返回批次，严格按index恢复输入顺序并拒绝缺项及重复项。 */
    private List<float[]> createEmbeddingBatch(EmbeddingModel model, List<String> batch,
            EmbeddingOptions options, int dimensions) {
        var request = new EmbeddingRequest(batch, options);
        var response = model.call(request);
        var ordered = new float[batch.size()][];
        for (var result : response.getResults()) {
            int index = result.getIndex();
            if (index < 0 || index >= ordered.length || ordered[index] != null) {
                throw new IllegalStateException("向量响应序号无效");
            }
            ordered[index] = result.getOutput();
        }
        List<float[]> vectors = new ArrayList<>();
        for (float[] vector : ordered) {
            requireVector(vector, dimensions);
            vectors.add(vector);
        }
        return vectors;
    }

    /**
     * @param connectionId 重排模型连接
     * @param query 用户问题
     * @param documents 带稳定分片ID的候选正文
     * @return 原生重排结果，调用方按ID关联并校验
     * @throws ApiRuntimeException 重排连接或调用不可用
     */
    public List<DocumentWithScore> createRerankList(Long connectionId, String query, List<Document> documents) {
        try {
            String tenantId = requireTenantId(connectionId);
            String key = connectionId.toString();
            var model = clients.getRerankModel(tenantId, key);
            // 当前原生重排由DashScope实现，显式请求全部候选，避免服务商默认Top N提前截断。
            var options = DashScopeRerankOptions.builder().topN(documents.size()).returnDocuments(false).build();
            var request = new RerankRequest(query, documents, options);
            return model.call(request).getResults();
        } catch (RuntimeException exception) {
            log.warn("模型重排调用失败，连接ID={}，异常类型={}", connectionId, exception.getClass().getSimpleName());
            throw new ApiRuntimeException("重排失败，请检查模型连接和服务状态后重试");
        }
    }

    /** 从持久化连接取得真实租户；平台管理员跨租户操作也不能使用当前会话租户解密。 */
    private String requireTenantId(Long id) {
        var connection = connections.selectById(id);
        if (connection == null) {
            throw new ApiRuntimeException("模型连接不存在");
        }
        return connection.getTenantId().toString();
    }

    /** 余弦检索不接受零向量、NaN、无穷值或错误维度，不能截断或补齐服务商输出。 */
    private void requireVector(float[] vector, int dimensions) {
        if (vector == null || vector.length != dimensions) {
            throw new IllegalStateException("向量响应维度不匹配");
        }
        double norm = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalStateException("向量包含非有限数值");
            }
            norm += (double) value * value;
        }
        if (norm == 0) {
            throw new IllegalStateException("向量不能为空向量");
        }
    }
}
