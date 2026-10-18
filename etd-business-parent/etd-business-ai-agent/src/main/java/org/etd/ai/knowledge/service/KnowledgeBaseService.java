package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.mapper.KnowledgeBaseMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

/** 知识库容器基础能力，基础阶段不创建模型客户端。 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private final KnowledgeBaseMapper mapper;

    /**
     * 获取当前租户的知识库分页。
     *
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param keyword 可选名称关键字，不作为SQL片段执行
     * @param dataStatus 可选启停状态；不传时不过滤状态
     * @return 符合当前租户及查询条件的分页结果
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public IPage<KnowledgeBaseEntity> fetchPage(
            long current, long size, String keyword, Integer dataStatus) {
        var page = new Page<KnowledgeBaseEntity>(current, size);
        var query =
                Wrappers.<KnowledgeBaseEntity>lambdaQuery()
                        .like(keyword != null, KnowledgeBaseEntity::getKnowledgeName, keyword)
                        .eq(dataStatus != null, KnowledgeBaseEntity::getDataStatus, dataStatus)
                        .orderByDesc(
                                KnowledgeBaseEntity::getCreateTime, KnowledgeBaseEntity::getId);
        return mapper.selectPage(page, query);
    }

    /**
     * 要求知识库存在；结构写事务可加行锁串行化目录操作。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param lock 是否在调用方事务内获取数据库行锁
     * @return 通过存在性及访问范围校验的业务记录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public KnowledgeBaseEntity requireBase(Long id, boolean lock) {
        var query = Wrappers.<KnowledgeBaseEntity>lambdaQuery().eq(KnowledgeBaseEntity::getId, id);
        if (lock) {
            query.last("FOR UPDATE");
        }
        var entity = mapper.selectOne(query);
        if (entity == null) {
            throw new ApiRuntimeException("知识库不存在");
        }
        return entity;
    }

    /**
     * 新建知识库；字段默认值由DTO转换及框架声明式填充提供。
     *
     * @param entity 本次写入的业务记录，租户和审计信息由后端控制
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public Long createBase(KnowledgeBaseEntity entity) {
        validateSettings(entity);
        mapper.insert(entity);
        return entity.getId();
    }

    /**
     * 按版本更新基础配置，拒绝并发覆盖。
     *
     * @param entity 本次写入的业务记录，租户和审计信息由后端控制
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void modifyBase(KnowledgeBaseEntity entity, Long version) {
        validateSettings(entity);
        entity.setVersion(version);
        int changed = mapper.updateById(entity);
        if (changed != 1) {
            throw new ApiRuntimeException("记录已变化，请刷新后重试");
        }
    }

    private void validateSettings(KnowledgeBaseEntity e) {
        if (e.getChunkMaxTokens() == null
                || e.getChunkOverlapTokens() == null
                || e.getKeywordTopK() == null
                || e.getVectorTopK() == null
                || e.getRetrievalTopK() == null
                || e.getChunkOverlapTokens() >= e.getChunkMaxTokens()) {
            throw new ApiRuntimeException("分片参数缺失或重叠数不小于分片上限");
        }
        long capacity =
                switch (e.getRetrievalMode()) {
                    case "keyword" -> e.getKeywordTopK();
                    case "vector" -> e.getVectorTopK();
                    default -> (long) e.getKeywordTopK() + e.getVectorTopK();
                };
        if (e.getRetrievalTopK() > capacity) {
            throw new ApiRuntimeException("最终返回数不能大于候选容量");
        }
    }

    /**
     * 删除已通过业务层非空校验的知识库。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void removeBase(Long id) {
        mapper.deleteById(id);
    }
}
