package org.etd.ai.knowledge.biz;

import lombok.RequiredArgsConstructor;

import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.service.*;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 目录结构写操作串行化于知识库行锁，避免并发移动产生环。 */
@Service
@RequiredArgsConstructor
public class KnowledgeBizService {
    private final KnowledgeBaseService bases;
    private final KnowledgeFolderService folders;

    /**
     * 新建目录需要主组织和目标父目录写权限。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public Long createFolder(Long baseId, KnowledgeFolderEntity e) {
        bases.requireBase(baseId, true);
        KnowledgeCreationPolicy.requireOrganization();
        folders.requireParent(baseId, e.getParentId());
        e.setKnowledgeBaseId(baseId);
        return folders.createFolder(e);
    }

    /**
     * 修改目录名称、位置或公开状态，不修改归属。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void modifyFolder(Long baseId, KnowledgeFolderEntity e, Long version) {
        bases.requireBase(baseId, true);
        var old = folders.requireWritable(baseId, e.getId());
        folders.requireParent(baseId, e.getParentId());
        folders.requireAcyclic(baseId, e.getId(), e.getParentId());
        if (e.getIsPublic() == null) {
            e.setIsPublic(old.getIsPublic());
        }
        e.setKnowledgeBaseId(baseId);
        folders.modifyFolder(e, version);
    }

    /**
     * 非空目录禁止删除，包括调用者不可见的子项。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void removeFolder(Long baseId, Long id) {
        bases.requireBase(baseId, true);
        folders.requireWritable(baseId, id);
        folders.removeFolder(baseId, id);
    }

    /**
     * 知识库仍有目录或文件时禁止删除，不进行隐式级联。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void removeBase(Long id) {
        bases.requireBase(id, true);
        if (folders.countFolder(id) > 0) {
            throw new ApiRuntimeException("知识库非空，请先清理目录和文件");
        }
        bases.removeBase(id);
    }
}
