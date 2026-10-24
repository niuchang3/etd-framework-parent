package org.etd.ai.knowledge.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import java.util.List;
/** 库内分类及树结构约束。 */
// 继承权限由知识库可信范围SQL/授权入口控制，避免对子表按上传人重复过滤；租户拦截仍启用。
@InterceptorIgnore(dataPermission = "true")
public interface KnowledgeFolderMapper extends BaseMapper<KnowledgeFolderEntity> {
    /** @param knowledgeBaseId 知识库 @param dataStatus 状态 @param scope 可信角色范围 @return 可见分类 */
    List<KnowledgeFolderEntity> selectFolderList(@Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("dataStatus") Integer dataStatus, @Param("scope") DataPermissionScope scope);
    /** @param id 分类 @param scope 可信范围 @return 可维护分类 */
    KnowledgeFolderEntity selectWritable(@Param("id") Long id, @Param("scope") DataPermissionScope scope);
    /** 串行保护分类树结构。 */
    void lockStructure();
    /** @param folderId 分类 @param targetId 目标 @return 祖先中出现次数 */
    Long selectAncestorCount(@Param("folderId") Long folderId, @Param("targetId") Long targetId);
    /** @param folderId 分类 @return 子目录数量 */
    Long selectChildCount(@Param("folderId") Long folderId);
    /** @param folderId 分类 @return 文档数量 */
    Long selectDocumentCount(@Param("folderId") Long folderId);
}
