package org.etd.ai.knowledge.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
/** 知识库内部分类能力，不具有独立组织数据权限。 */
// 分类无独立组织字段，查询通过关联知识库交由框架授权。
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_base"))
@Service
@RequiredArgsConstructor
public class KnowledgeFolderService {
    private final KnowledgeFolderMapper mapper;
    /** @param knowledgeBaseId 知识库 @param dataStatus 可选目录状态 @return 可见目录与必要祖先 */
    @DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_base", publicColumn = KnowledgePolicy.PUBLIC_COLUMN))
    public List<KnowledgeFolderEntity> fetchFolderList(Long knowledgeBaseId, Integer dataStatus) {
        List<KnowledgeFolderEntity> records = mapper.selectFolderList(knowledgeBaseId, dataStatus);
        DataPermissionScope scope = DataPermissionScope.current();
        for (KnowledgeFolderEntity folder : records) {
            folder.setWritable(scope.canManage(folder.getPermissionOrgId(), folder.getPermissionCreateBy()));
        }
        return records;
    }
    /** @param id 分类ID @return 继承知识库维护资格的分类 */
    public KnowledgeFolderEntity requireWritable(Long id) {
        KnowledgeFolderEntity entity = mapper.selectWritable(id);
        if (entity == null) {
            throw new ApiRuntimeException("分类不存在或无知识库维护权限");
        }
        entity.setWritable(true);
        return entity;
    }
    /** @param parentId 可选父目录 @param knowledgeBaseId 所属知识库 */
    public void requireParent(Long parentId, Long knowledgeBaseId) {
        if (parentId != null) {
            requireFolderByBase(parentId, knowledgeBaseId);
        }
    }
    /** @param id 分类 @param knowledgeBaseId 预期归属 @return 已授权分类 */
    public KnowledgeFolderEntity requireFolderByBase(Long id, Long knowledgeBaseId) {
        KnowledgeFolderEntity entity = requireWritable(id);
        if (!entity.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new ApiRuntimeException("分类不属于当前知识库");
        }
        return entity;
    }
    /** 分类结构修改在事务中串行化，避免并发形成环。 */
    public void lockStructure() { mapper.lockStructure(); }
    /** @param id 当前目录 @param targetId 目标父目录 */
    public void requireAcyclic(Long id, Long targetId) {
        if (targetId != null && mapper.selectAncestorCount(id, targetId) > 0) {
            throw new ApiRuntimeException("不能移入自身或下级目录");
        }
    }
    /** @param entity 已由Biz校验知识库权限的分类 @return ID */
    public Long createFolder(KnowledgeFolderEntity entity) { mapper.insert(entity); return entity.getId(); }
    /** @param entity 已授权分类 @param version 当前版本 */
    public void modifyFolder(KnowledgeFolderEntity entity, Long version) {
        entity.setVersion(version);
        if (mapper.updateById(entity) != 1) {
            throw new ApiRuntimeException("分类已变化，请刷新后重试");
        }
    }
    /** @param knowledgeBaseId 已校验维护权限且无文档的知识库，清理其分类。 */
    public void removeFolderList(Long knowledgeBaseId) {
        LambdaQueryWrapper<KnowledgeFolderEntity> query = Wrappers.<KnowledgeFolderEntity>lambdaQuery()
                .eq(KnowledgeFolderEntity::getKnowledgeBaseId, knowledgeBaseId);
        mapper.delete(query);
    }
    /** @param id 已授权分类，有子目录或文档时禁止删除 */
    public void removeFolder(Long id) {
        if (mapper.selectChildCount(id) > 0 || mapper.selectDocumentCount(id) > 0) {
            throw new ApiRuntimeException("分类包含子分类或文档，不能删除");
        }
        mapper.deleteById(id);
    }
}
