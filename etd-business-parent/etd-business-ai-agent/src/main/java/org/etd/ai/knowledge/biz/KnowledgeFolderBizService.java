package org.etd.ai.knowledge.biz;

import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 独立目录维护用例，结构变更统一在事务内串行化。 */
@Service
@RequiredArgsConstructor
public class KnowledgeFolderBizService {
    private final KnowledgeFolderService folders;

    /** @param entity 新目录信息 @return 新建目录标识 */
    @Transactional
    public Long createFolder(KnowledgeFolderEntity entity) {
        folders.lockStructure();
        folders.requireParent(entity.getParentId());
        return folders.createFolder(entity);
    }

    /** @param entity 目录编辑字段 @param version 当前乐观锁版本 */
    @Transactional
    public void modifyFolder(KnowledgeFolderEntity entity, Long version) {
        folders.lockStructure();
        folders.requireWritable(entity.getId());
        folders.requireParent(entity.getParentId());
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
