package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;

import java.util.List;

/** 目录业务查询；可读来源由Service注解过滤，结构完整性查询保留独立边界。 */
public interface KnowledgeFolderMapper extends BaseMapper<KnowledgeFolderEntity> {
    /**
     * 返回可读目录及必要祖先，导航祖先不因此获得写入权限。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param dataStatus 可选目录状态；递归导航祖先不按状态裁剪
     * @return 符合条件的记录集合，不在Java中拼接跨表权限数据
     */
    List<KnowledgeFolderEntity> selectReadableFolderList(
            @Param("baseId") Long baseId, @Param("dataStatus") Integer dataStatus);

    /**
     * 在所属知识库已加事务锁后检查移动目标是否属于当前目录后代。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param folderId 目录标识，空表示未分类或全库统计，具体见方法用途
     * @param targetId 待移动到的目标目录标识
     * @return 符合当前租户及查询条件的记录数量
     */
    @InterceptorIgnore(dataPermission = "true")
    Long selectAncestorCount(
            @Param("baseId") Long baseId,
            @Param("folderId") Long folderId,
            @Param("targetId") Long targetId);

    /**
     * 非空判断必须包含当前用户不可见的子项；调用前业务已验证目录管理权限。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param folderId 目录标识，空表示未分类或全库统计，具体见方法用途
     * @return 符合当前租户及查询条件的记录数量
     */
    @InterceptorIgnore(dataPermission = "true")
    Long selectChildCount(@Param("baseId") Long baseId, @Param("folderId") Long folderId);
}
