package org.etd.ai.knowledge.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import java.util.List;
/** 库内分类及树结构约束。 */
public interface KnowledgeFolderMapper extends BaseMapper<KnowledgeFolderEntity> {
    /** @param knowledgeBaseId 知识库 @param dataStatus 状态 @return 可见分类 */
    List<KnowledgeFolderEntity> selectFolderList(@Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("dataStatus") Integer dataStatus);
    /** @param id 分类 @return 可维护分类 */
    KnowledgeFolderEntity selectWritable(@Param("id") Long id);
    /** 串行保护分类树结构。 */
    void lockStructure();
    /** @param folderId 分类 @param targetId 目标 @return 祖先中出现次数 */
    Long selectAncestorCount(@Param("folderId") Long folderId, @Param("targetId") Long targetId);
    /** @param folderId 分类 @return 子目录数量 */
    Long selectChildCount(@Param("folderId") Long folderId);
    /** @param folderId 分类 @return 文档数量 */
    Long selectDocumentCount(@Param("folderId") Long folderId);
}
