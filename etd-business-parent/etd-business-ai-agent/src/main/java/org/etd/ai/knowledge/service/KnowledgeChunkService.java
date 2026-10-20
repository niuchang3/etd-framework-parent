package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.mapper.KnowledgeChunkMapper;
import org.springframework.stereotype.Service;
import java.util.List;

/** 正文分片能力，访问权限由调用方锁定并校验资料后确定。 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkService {
    private final KnowledgeChunkMapper mapper;
    /** @param documentId 已校验资料 @param version 当前轮次 @return 当前正文分片 */
    public List<KnowledgeChunkEntity> fetchChunkList(Long documentId, Long version) {
        var query = Wrappers.<KnowledgeChunkEntity>lambdaQuery()
                .eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .eq(KnowledgeChunkEntity::getProcessingVersion, version)
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        return mapper.selectList(query);
    }
    /** @param documentId 已校验资料 @param version 当前轮次 @param current 页码 @param size 每页数量 @return 分片分页 */
    public IPage<KnowledgeChunkEntity> fetchChunkPage(Long documentId, Long version, long current, long size) {
        var query = Wrappers.<KnowledgeChunkEntity>lambdaQuery().eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .eq(KnowledgeChunkEntity::getProcessingVersion, version).orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        var page = new Page<KnowledgeChunkEntity>(current, size);
        return mapper.selectPage(page, query);
    }
    /** @param documentId 已锁定资料；清理旧轮次正文及向量 */
    public void removeChunkList(Long documentId) {
        for (int dimension : KnowledgePolicy.DIMENSIONS) {
            mapper.deleteDocumentVectors(documentId, dimension);
        }
        mapper.deleteDocumentChunks(documentId);
    }
    /** @param documentId 已锁定资料；仅清理向量，保留待确认正文 */
    public void removeVectorList(Long documentId) {
        for (int dimension : KnowledgePolicy.DIMENSIONS) {
            mapper.deleteDocumentVectors(documentId, dimension);
        }
    }
    /** @param records 本轮分片；整体发布前更新索引状态 */
    public void modifyChunkList(List<KnowledgeChunkEntity> records) {
        for (KnowledgeChunkEntity chunk : records) {
            // 已加载实体的旧审计值清空后，由框架按当前请求重新填充。
            chunk.setUpdateTime(null);
            chunk.setUpdateBy(null);
            if (mapper.updateById(chunk) != 1) {
                throw new ApiRuntimeException("正文分片已变化，请重新处理");
            }
        }
    }
    /** @param chunks 当前轮次的待写入正文分片 */
    public void createChunkList(List<KnowledgeChunkEntity> chunks) {
        for (KnowledgeChunkEntity chunk : chunks) {
            mapper.insert(chunk);
        }
    }
}
