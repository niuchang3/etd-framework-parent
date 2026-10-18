package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

import java.util.List;

/** 租户级独立目录维护能力。 */
@Service
@RequiredArgsConstructor
public class KnowledgeFolderService {
    private final KnowledgeFolderMapper mapper;

    /**
     * 查询租户目录和必要祖先，保留完整导航路径。
     * @param dataStatus 可选目录状态，必要祖先不受筛选限制
     * @return 平铺目录节点列表
     */
    public List<KnowledgeFolderEntity> fetchFolderList(Integer dataStatus) {
        return mapper.selectFolderList(dataStatus);
    }

    /**
     * 要求目录存在且在当前租户范围中。
     * @param id 目录标识
     * @return 可管理的目录记录
     * @throws ApiRuntimeException 目录不存在时
     */
    public KnowledgeFolderEntity requireWritable(Long id) {
        var query = Wrappers.<KnowledgeFolderEntity>lambdaQuery().eq(KnowledgeFolderEntity::getId, id);
        var entity = mapper.selectOne(query);
        if (entity == null) {
            throw new ApiRuntimeException("目录不存在");
        }
        return entity;
    }

    /**
     * 目录父节点可为空；非空父节点必须属于可访问租户。
     * @param parentId 目标父目录标识，空表示根目录
     */
    public void requireParent(Long parentId) {
        if (parentId != null) {
            requireWritable(parentId);
        }
    }

    /**
     * 串行化结构变更，防止并发目录移动形成环，以及删除目录时新增子目录。
     * 调用方必须处于事务内，数据库在事务结束时自动释放锁。
     */
    public void lockStructure() {
        mapper.lockStructure();
    }

    /**
     * 检查移动目标的祖先链，禁止移入自身或后代。
     * @param id 当前目录标识
     * @param targetId 目标父目录，空表示根目录
     * @throws ApiRuntimeException 移动会形成循环时
     */
    public void requireAcyclic(Long id, Long targetId) {
        if (targetId != null && mapper.selectAncestorCount(id, targetId) > 0) {
            throw new ApiRuntimeException("不能移入自身或下级目录");
        }
    }

    /**
     * 创建目录，归属及默认状态由框架填充。
     * @param entity 目录基本信息
     * @return 新建目录标识
     */
    public Long createFolder(KnowledgeFolderEntity entity) {
        mapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新目录并通过乐观锁拒绝并发覆盖，父节点允许显式清空。
     * @param entity 本次编辑字段
     * @param version 前端读取的当前版本
     */
    public void modifyFolder(KnowledgeFolderEntity entity, Long version) {
        entity.setVersion(version);
        if (mapper.updateById(entity) != 1) {
            throw new ApiRuntimeException("目录已变化，请刷新后重试");
        }
    }

    /**
     * 删除无子目录占用的目录，检查包括当前用户不可见的记录。
     * @param id 已校验管理权限的目录标识
     */
    public void removeFolder(Long id) {
        if (mapper.selectChildCount(id) > 0) {
            throw new ApiRuntimeException("目录包含子目录，不能删除");
        }
        mapper.deleteById(id);
    }
}
