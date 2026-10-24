package org.etd.ai.knowledge.biz;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.component.KnowledgeModelClient;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.mapper.KnowledgeBaseMapper;
import org.etd.ai.knowledge.mapper.KnowledgeRetrievalMapper;
import org.etd.ai.knowledge.model.KnowledgeSearchHit;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;

/** 数据库持久化构建状态与租约驱动的后台任务；重启后按代次安全重试，不依赖内存队列。 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "etd.ai.knowledge.index.enabled", matchIfMissing = true)
public class KnowledgeIndexBuildBizService {
    private final KnowledgeBaseMapper bases;
    private final KnowledgeRetrievalMapper vectors;
    private final KnowledgeModelClient models;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    /** 内部轮询跨租户任务，单个任务设置独立租户上下文，结束后清理。 */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void buildPendingIndexes() {
        for (var pending : vectors.selectPendingBuildList()) {
            if (vectors.claimBuild(pending.getId(), pending.getBuildingIndexVersion()) != 1) {
                continue;
            }
            try {
                RequestContext.setTenantCode(pending.getTenantId());
                var task = bases.selectById(pending.getId());
                buildIndex(task);
            } catch (Exception exception) {
                // 不记录模型异常正文、请求载荷、密钥及资料内容。
                log.error("知识库索引任务异常，知识库ID={}，异常类型={}", pending.getId(), exception.getClass().getSimpleName());
            } finally {
                RequestContext.clean();
            }
        }
    }

    /** 清理失效索引代次，不删除当前索引，也不影响正在构建的候选索引。 */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanRetiredIndexes() {
        for (Integer dimensions : KnowledgeRetrievalPolicy.DIMENSIONS) {
            String table = KnowledgeRetrievalPolicy.vectorTable(dimensions);
            vectors.deleteRetiredVectors(table);
        }
    }

    /** @param task 已领取租约的任务，仅由后台入口调用 */
    public void buildIndex(KnowledgeBaseEntity task) {
        try {
            String table = KnowledgeRetrievalPolicy.vectorTable(task.getDimensions());
            var records = vectors.selectBuildChunkList(task);
            task.setIndexTotal(records.size());
            updateProgress(task, 0);
            int completed = 0;
            for (var chunk : records) {
                requireLease(task);
                if (vectors.selectExistingVectorCount(task, table, chunk) == 0) {
                    storeChunk(task, table, chunk);
                }
                updateProgress(task, ++completed);
            }
            transactions.executeWithoutResult(status -> publishIndex(task, table));
        } catch (Exception exception) {
            var update = taskUpdate(task).set(KnowledgeBaseEntity::getIndexStatus, KnowledgeRetrievalPolicy.FAILED)
                    .set(KnowledgeBaseEntity::getIndexError, "构建失败，请检查模型连接、内容变更及服务状态后重新初始化")
                    .set(KnowledgeBaseEntity::getIndexLeaseUntil, null);
            bases.update(null, update);
            log.warn("知识库索引构建失败，知识库ID={}，代次={}，异常类型={}", task.getId(),
                    task.getBuildingIndexVersion(), exception.getClass().getSimpleName());
        }
    }

    /** 恢复任务时先复用已落库结果，避免服务重启后对相同内容重复计费。 */
    private void storeChunk(KnowledgeBaseEntity task, String table, KnowledgeSearchHit chunk) throws Exception {
        var embedding = models.embed(task.getEmbeddingConnectionId(), task.getDimensions(), chunk.getContent(), false);
        String metadata = createMetadata(task, chunk);
        String vector = Arrays.toString(embedding);
        Long vectorId = IdWorker.getId();
        vectors.insertVector(table, vectorId, chunk.getContent(), metadata, vector);
    }

    /** 发布前复核全部确认内容，正文变化不得把未覆盖的新内容误标记为就绪。 */
    private void publishIndex(KnowledgeBaseEntity task, String table) {
        requireLease(task);
        if (vectors.selectMissingCount(task, table) != 0) {
            throw new ApiRuntimeException("构建期间文档发生变化，请重试");
        }
        var update = taskUpdate(task)
                .set(KnowledgeBaseEntity::getActiveConnectionId, task.getEmbeddingConnectionId())
                .set(KnowledgeBaseEntity::getActiveDimensions, task.getDimensions())
                .set(KnowledgeBaseEntity::getActiveIndexVersion, task.getBuildingIndexVersion())
                .set(KnowledgeBaseEntity::getIndexStatus, KnowledgeRetrievalPolicy.READY)
                .set(KnowledgeBaseEntity::getIndexError, null)
                .set(KnowledgeBaseEntity::getIndexLeaseUntil, null)
                .set(KnowledgeBaseEntity::getUpdateTime, Instant.now());
        if (bases.update(null, update) != 1) {
            throw new ApiRuntimeException("本次构建已失效");
        }
    }

    private void updateProgress(KnowledgeBaseEntity task, int completed) {
        var update = taskUpdate(task).set(KnowledgeBaseEntity::getIndexTotal, task.getIndexTotal())
                .set(KnowledgeBaseEntity::getIndexCompleted, completed);
        if (bases.update(null, update) != 1) {
            throw new ApiRuntimeException("构建租约已变化");
        }
    }

    private void requireLease(KnowledgeBaseEntity task) {
        if (task.getIndexLeaseUntil() == null || !task.getIndexLeaseUntil().isAfter(Instant.now())) {
            throw new ApiRuntimeException("本次构建租约到期，请等待自动恢复");
        }
    }

    /** 条件绑定代次与本次租约，旧工作线程不能覆盖重新领取的任务。 */
    private LambdaUpdateWrapper<KnowledgeBaseEntity> taskUpdate(KnowledgeBaseEntity task) {
        return Wrappers.<KnowledgeBaseEntity>lambdaUpdate().eq(KnowledgeBaseEntity::getId, task.getId())
                .eq(KnowledgeBaseEntity::getBuildingIndexVersion, task.getBuildingIndexVersion())
                .eq(KnowledgeBaseEntity::getIndexLeaseUntil, task.getIndexLeaseUntil())
                .eq(KnowledgeBaseEntity::getIndexStatus, KnowledgeRetrievalPolicy.BUILDING);
    }

    private String createMetadata(KnowledgeBaseEntity base, KnowledgeSearchHit chunk) throws Exception {
        var metadata = Map.of("tenant_id", base.getTenantId().toString(), "knowledge_base_id", base.getId().toString(),
                "document_id", chunk.getDocumentId().toString(), "chunk_id", chunk.getChunkId().toString(),
                "model_connection_id", base.getEmbeddingConnectionId().toString(),
                "index_version", base.getBuildingIndexVersion().toString(),
                "processing_version", chunk.getProcessingVersion().toString());
        return json.writeValueAsString(metadata);
    }
}
