package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;

import java.util.List;

/** 独立目录树查询及结构约束。 */
public interface KnowledgeFolderMapper extends BaseMapper<KnowledgeFolderEntity> {
    /** @param orgId 所选部门标识 @param dataStatus 可选目录状态 @return 部门目录及必要祖先列表 */
    List<KnowledgeFolderEntity> selectFolderList(@Param("orgId") Long orgId, @Param("dataStatus") Integer dataStatus);

    /** 结构变更共用事务锁，不接收客户端锁键，不改变租户或数据权限上下文。 */
    void lockStructure();

    /**
     * 内部结构检查覆盖隐藏祖先；调用前已校验待移动目录及目标父目录的管理权限。
     * @param folderId 当前目录标识
     * @param targetId 待移动到的父目录
     * @return 祖先链中当前目录的出现次数
     */
    @InterceptorIgnore(dataPermission = "true")
    Long selectAncestorCount(@Param("folderId") Long folderId, @Param("targetId") Long targetId);

    /**
     * 内部非空检查包括不可见子目录，调用前已校验当前目录管理权限。
     * @param folderId 已校验管理权限的目录标识
     * @return 有效子目录数量
     */
    @InterceptorIgnore(dataPermission = "true")
    Long selectChildCount(@Param("folderId") Long folderId);
}
