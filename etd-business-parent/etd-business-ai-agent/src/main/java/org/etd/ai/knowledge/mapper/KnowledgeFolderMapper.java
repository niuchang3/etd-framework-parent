package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;

import java.util.List;

/** 独立目录树查询及结构约束。 */
public interface KnowledgeFolderMapper extends BaseMapper<KnowledgeFolderEntity> {
    /** @param dataStatus 可选目录状态 @return 可维护目录及必要祖先列表 */
    List<KnowledgeFolderEntity> selectFolderList(@Param("dataStatus") Integer dataStatus);

    /** 结构变更共用事务锁，不接收客户端锁键，不改变租户或数据权限上下文。 */
    void lockStructure();

    /**
     * 检查移动是否产生目录环，租户范围由框架限制。
     * @param folderId 当前目录标识
     * @param targetId 待移动到的父目录
     * @return 祖先链中当前目录的出现次数
     */
    Long selectAncestorCount(@Param("folderId") Long folderId, @Param("targetId") Long targetId);

    /**
     * 检查有效子目录，租户隔离仍由框架负责。
     * @param folderId 已校验管理权限的目录标识
     * @return 有效子目录数量
     */
    Long selectChildCount(@Param("folderId") Long folderId);
}
