package org.etd.ai.knowledge.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.*;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/** 资料配置、移动和删除用例，跨服务操作在事务中编排。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentBizService {
    private final KnowledgeDocumentService documents;
    private final KnowledgeFolderService folders;
    private final KnowledgeChunkService chunks;
    private final KnowledgeObjectService objects;
    private final ModelConnectionService connections;
    private final ModelService models;

    /** @param id 资料ID @param input 可编辑配置 @param version 当前版本 */
    @Transactional
    public void modifyDocument(Long id, KnowledgeDocumentEntity input, Long version) {
        folders.lockStructure();
        var document = documents.requireDocument(id, true);
        requireIdleProcessing(document);
        if (!Objects.equals(document.getVersion(), version)) {
            throw new ApiRuntimeException("资料已变化，请刷新后重试");
        }
        var folder = folders.requireWritable(input.getFolderId());
        if (folder.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE) {
            throw new ApiRuntimeException("不能移入停用目录");
        }
        validateConfiguration(input);
        boolean rebuild = hasChangedProcessingConfiguration(document, input);
        KnowledgeDocumentConverter.INSTANCE.updateEntity(input, document);
        document.setEmbeddingModelId(input.getEmbeddingModelId());
        if (rebuild || document.getProcessingOwner() != null) {
            invalidateProcessing(document);
        }
        documents.modifyDocument(document);
        // 权限公开标记和标题变化也重建，确保正文、向量与资料一致，不保留双套数据。
        if (rebuild) {
            chunks.removeChunkList(id);
        }
    }

    /** @param id 资料ID；清理失败保留资料记录，允许用户重试删除 */
    @Transactional
    public void removeDocument(Long id) {
        var document = documents.requireDocument(id, true);
        requireIdleProcessing(document);
        try {
            objects.abortUpload(document);
            objects.removeObject(document);
        } catch (Exception exception) {
            log.error("知识资料原件删除失败，资料ID={}，异常类型={}", id, exception.getClass().getSimpleName());
            throw new ApiRuntimeException("原件清理失败，请重试删除");
        }
        chunks.removeChunkList(id);
        documents.removeDocument(id);
    }

    /** @param id 已上传资料ID @return 受鉴权保护的临时下载地址 */
    public String createDownloadUrl(Long id) {
        var document = documents.requireDocument(id, false);
        if (!KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())) {
            throw new ApiRuntimeException("文件尚未上传完成");
        }
        try {
            return objects.generateDownloadUrl(document);
        } catch (Exception exception) {
            throw new ApiRuntimeException("生成下载地址失败，请重试");
        }
    }

    /** @param document 待保存配置；校验分片与当前平台模型登记的维度 */
    public void validateConfiguration(KnowledgeDocumentEntity document) {
        if (document.getChunkOverlapTokens() >= document.getChunkMaxTokens()) {
            throw new ApiRuntimeException("分片重叠Token数必须小于分片上限");
        }
        int candidates = switch (document.getRetrievalMode()) {
            case KnowledgePolicy.KEYWORD -> document.getKeywordTopK();
            case KnowledgePolicy.VECTOR -> document.getVectorTopK();
            default -> document.getKeywordTopK() + document.getVectorTopK();
        };
        if (document.getRetrievalTopK() > candidates) {
            throw new ApiRuntimeException("最终召回数量不能超过当前检索方式的候选数量");
        }
        if (document.getEmbeddingModelConnectionId() == null) {
            if (!KnowledgePolicy.KEYWORD.equals(document.getRetrievalMode()) || document.getEmbedding() != null) {
                throw new ApiRuntimeException("向量或混合检索必须配置模型连接器和维度");
            }
            document.setEmbeddingModelId(null);
            return;
        }
        var connection = connections.requireConnection(document.getEmbeddingModelConnectionId(), false);
        var model = models.requireModel(connection.getModelId());
        if (connection.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE
                || !ModelType.EMBEDDING.equals(model.getModelType()) || document.getEmbedding() == null
                || !KnowledgePolicy.DIMENSIONS.contains(document.getEmbedding())
                || model.getEmbedding() == null || !Arrays.asList(model.getEmbedding()).contains(document.getEmbedding())) {
            throw new ApiRuntimeException("连接器未启用、不是向量模型或不支持所选维度");
        }
        document.setEmbeddingModelId(model.getId());
    }

    /** @param document 资料；有效租约内拒绝修改和删除，过期可重新处理 */
    public void requireIdleProcessing(KnowledgeDocumentEntity document) {
        if (document.getProcessingOwner() != null && document.getProcessingLeaseUntil() != null
                && document.getProcessingLeaseUntil().isAfter(Instant.now())) {
            throw new ApiRuntimeException("资料正在处理中，请稍后重试");
        }
    }

    /** 配置及权限副本变化后使旧轮次失效；纯目录移动、描述和召回数量变化不重建。 */
    private boolean hasChangedProcessingConfiguration(KnowledgeDocumentEntity old, KnowledgeDocumentEntity input) {
        return !Objects.equals(old.getChunkMaxTokens(), input.getChunkMaxTokens())
                || !Objects.equals(old.getChunkOverlapTokens(), input.getChunkOverlapTokens())
                || !Objects.equals(old.getEmbeddingModelConnectionId(), input.getEmbeddingModelConnectionId())
                || !Objects.equals(old.getEmbedding(), input.getEmbedding())
                || !Objects.equals(old.getRetrievalMode(), input.getRetrievalMode())
                || !Objects.equals(old.getIsPublic(), input.getIsPublic())
                || !Objects.equals(old.getDocumentName(), input.getDocumentName());
    }

    /** 废弃旧轮次，防止参数改变后旧向量继续参与召回。 */
    private void invalidateProcessing(KnowledgeDocumentEntity document) {
        document.setProcessingVersion(document.getProcessingVersion() + 1);
        document.setProcessingStatus(KnowledgePolicy.UPLOADED);
        document.setProcessingOwner(null);
        document.setProcessingLeaseUntil(null);
        document.setErrorMessage(null);
        document.setProcessedTime(null);
    }
}
