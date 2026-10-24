package org.etd.ai.knowledge.biz;

import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.springframework.stereotype.Service;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.springframework.transaction.annotation.Transactional;

/** 知识库内部分类维护用例，结构变更统一在事务内串行化。 */
@Service
@RequiredArgsConstructor
public class KnowledgeFolderBizService {
    private final KnowledgeFolderService folders;
    private final KnowledgeBaseService bases;

    /** @param entity 新目录信息 @return 新建目录标识 */
    @Transactional
    public Long createFolder(KnowledgeFolderEntity entity) {
        folders.lockStructure();
        bases.requireBase(entity.getKnowledgeBaseId(), true, true);
        folders.requireParent(entity.getParentId(), entity.getKnowledgeBaseId());
        return folders.createFolder(entity);
    }

    /** @param entity 目录编辑字段 @param version 当前乐观锁版本 */
    @Transactional
    public void modifyFolder(KnowledgeFolderEntity entity, Long version) {
        folders.lockStructure();
        bases.requireBase(entity.getKnowledgeBaseId(), true, true);
        folders.requireFolderByBase(entity.getId(), entity.getKnowledgeBaseId());
        folders.requireParent(entity.getParentId(), entity.getKnowledgeBaseId());
        folders.requireAcyclic(entity.getId(), entity.getParentId());
        folders.modifyFolder(entity, version);
    }

    /** @param id 要删除的目录；有效子目录会阻止删除 */
    @Transactional
    public void removeFolder(Long id) {
        folders.lockStructure();
        folders.requireWritable(id);
        folders.removeFolder(id);
    }
}
