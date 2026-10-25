package org.etd.ai.knowledge.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeVectorService;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/** 资料配置、移动和删除用例，跨服务操作在事务中编排。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentBizService {
    private final KnowledgeDocumentService documents;
    private final KnowledgeFolderService folders;
    private final KnowledgeObjectService objects;
    private final KnowledgeChunkService chunks;
    private final KnowledgeVectorService vectors;

    /** @param id 资料ID @param input 可编辑配置 @param version 当前版本 */
    @Transactional
    public void modifyDocument(Long id, KnowledgeDocumentEntity input, Long version) {
        folders.lockStructure();
        var document = documents.requireDocument(id, true);
        documents.requireChunkIdle(document);
        if (!Objects.equals(document.getVersion(), version)) {
            throw new ApiRuntimeException("资料已变化，请刷新后重试");
        }
        var folder = folders.requireFolderByBase(input.getFolderId(), document.getKnowledgeBaseId());
        if (folder.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE) {
            throw new ApiRuntimeException("不能移入停用目录");
        }
        if (!Objects.equals(document.getIsPublic(), input.getIsPublic())) {
            chunks.modifyPublicByDocument(id, input.getIsPublic());
        }
        KnowledgeDocumentConverter.INSTANCE.updateEntity(input, document);
        documents.modifyDocument(document);
    }

    /** @param id 资料ID；清理失败保留资料记录，允许用户重试删除 */
    @Transactional
    public void removeDocument(Long id) {
        var document = documents.requireDocument(id, true);
        documents.requireChunkIdle(document);
        try {
            objects.abortUpload(document);
            objects.removeObject(document);
        } catch (Exception exception) {
            log.error("知识资料原件删除失败，资料ID={}，异常类型={}", id, exception.getClass().getSimpleName());
            throw new ApiRuntimeException("原件清理失败，请重试删除");
        }
        vectors.removeVectorList(id);
        chunks.removeChunkList(id);
        documents.removeDocument(id);
    }

    /** @param id 文档 @param version 当前版本 @param dataStatus 文档启停，启用前须完成上传并确认分片 */
    @Transactional
    public void modifyDocumentStatus(Long id, Long version, Integer dataStatus) {
        var document = documents.requireDocument(id, true);
        if (!Objects.equals(document.getVersion(), version)) {
            throw new ApiRuntimeException("文档已变化，请刷新后重试");
        }
        if (dataStatus == BasicConstant.DataStatus.ENABLED_CODE) {
            if (!KnowledgeChunkStatus.CONFIRMED.equals(document.getChunkStatus()) || document.getChunkCount() == 0
                    || !KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())) {
                throw new ApiRuntimeException("请先完成上传并确认有效分片");
            }
        }
        document.setDataStatus(dataStatus);
        documents.modifyDocument(document);
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
}
