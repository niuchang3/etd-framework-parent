package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.mapper.KnowledgeChunkMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.springframework.stereotype.Service;
import java.util.List;

/** 分片持久化能力；事务、资料权限及整份结果的版本由Biz层统一控制。 */
@Service
@RequiredArgsConstructor
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_chunk", publicColumn = KnowledgePolicy.PUBLIC_COLUMN))
public class KnowledgeChunkService {
    private final KnowledgeChunkMapper mapper;
    /** @param documentId 已授权资料ID @param current 页码 @param size 单页数量 @return 按原文顺序排列的分片 */
    public IPage<KnowledgeChunkEntity> fetchChunkPage(Long documentId, long current, long size) {
        var query = Wrappers.<KnowledgeChunkEntity>lambdaQuery().eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        var page = new Page<KnowledgeChunkEntity>(current, size);
        return mapper.selectPage(page, query);
    }
    /** @param documentId 已授权资料ID @return 当前全部分片，顺序稳定 */
    public List<KnowledgeChunkEntity> fetchChunkList(Long documentId) {
        var query = Wrappers.<KnowledgeChunkEntity>lambdaQuery().eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        return mapper.selectList(query);
    }
    /** @param documentId 已授权资料ID @param id 分片ID @return 本资料内的分片，不允许跨资料访问 */
    public KnowledgeChunkEntity requireChunk(Long documentId, Long id) {
        var query = Wrappers.<KnowledgeChunkEntity>lambdaQuery().eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .eq(KnowledgeChunkEntity::getId, id);
        var entity = mapper.selectOne(query);
        if (entity == null) { throw new ApiRuntimeException("分片不存在或不属于当前资料"); }
        return entity;
    }
    /** @param records 可信归属及正文已确定的新结果 */
    public void createChunkList(List<KnowledgeChunkEntity> records) {
        for (KnowledgeChunkEntity record : records) {
            record.setId(IdWorker.getId());
            mapper.insert(record);
        }
    }
    /** @param record 已加载并校验的分片 */
    public void modifyChunk(KnowledgeChunkEntity record) {
        record.setUpdateTime(null);
        record.setUpdateBy(null);
        if (mapper.updateById(record) != 1) { throw new ApiRuntimeException("分片已变化，请刷新后重试"); }
    }
    /** @param id 已校验的分片ID，合并时移除被吸收的分片 */
    public void removeChunk(Long id) {
        if (mapper.deleteById(id) != 1) { throw new ApiRuntimeException("分片已变化，请刷新后重试"); }
    }
    /** @param documentId 已校验维护权限的资料ID @param isPublic 新共享标记；与资料更新处于同一事务 */
    public void modifyPublicByDocument(Long documentId, Boolean isPublic) {
        var changes = new KnowledgeChunkEntity();
        changes.setIsPublic(isPublic);
        var update = Wrappers.<KnowledgeChunkEntity>lambdaUpdate()
                .eq(KnowledgeChunkEntity::getDocumentId, documentId);
        mapper.update(changes, update);
    }
    /** @param documentId 已授权资料ID，仅在生成成功或资料删除时清理 */
    public void removeChunkList(Long documentId) { mapper.deleteByDocument(documentId); }
}
