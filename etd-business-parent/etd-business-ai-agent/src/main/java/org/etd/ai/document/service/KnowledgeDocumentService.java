package org.etd.ai.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.etd.ai.document.entity.KnowledgeDocumentEntity;
import org.etd.ai.document.mapper.KnowledgeDocumentMapper;
import org.etd.ai.knowledge.constant.AiStage;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.springframework.stereotype.Service;

/** 文档基础信息与清理能力，读取权限由SQL在分页前执行。 */
@Service
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_document"))
@RequiredArgsConstructor
public class KnowledgeDocumentService {
    private final KnowledgeDocumentMapper mapper;

    /**
     * 按目录、未分类或全部文件分页。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param folderId 目录标识，空表示未分类或全库统计，具体见方法用途
     * @param uncategorized 是否仅查询未分类文件，不能与目录筛选并用
     * @param keyword 可选名称关键字，不作为SQL片段执行
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param dataStatus 可选文档启停状态，不筛选所属知识库状态
     * @return 符合当前租户及查询条件的分页结果
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @DataPermission(
            tables =
                    @DataPermissionTable(
                            table = "ai_knowledge_document",
                            publicColumn = "is_public"))
    public IPage<KnowledgeDocumentEntity> fetchPage(
            Long baseId,
            Long folderId,
            boolean uncategorized,
            String keyword,
            long current,
            long size,
            Integer dataStatus) {
        var page = new Page<KnowledgeDocumentEntity>(current, size);
        return mapper.selectReadablePage(
                page, baseId, folderId, uncategorized, keyword, dataStatus);
    }

    /**
     * 下载、预览与详情统一从可读文档入口取得权威记录。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过存在性及访问范围校验的业务记录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @DataPermission(
            tables =
                    @DataPermissionTable(
                            table = "ai_knowledge_document",
                            publicColumn = "is_public"))
    public KnowledgeDocumentEntity requireReadable(Long baseId, Long id) {
        var e = mapper.selectReadableById(id, baseId);
        if (e == null) {
            throw new ApiRuntimeException("文件不存在或无读取权限");
        }
        return e;
    }

    /**
     * 写入口继续使用框架角色范围，不通过公开状态扩权。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param lock 是否在调用方事务内获取数据库行锁
     * @return 通过存在性及访问范围校验的业务记录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public KnowledgeDocumentEntity requireWritable(Long baseId, Long id, boolean lock) {
        var q =
                Wrappers.<KnowledgeDocumentEntity>lambdaQuery()
                        .eq(KnowledgeDocumentEntity::getId, id)
                        .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, baseId);
        if (lock) {
            q.last("FOR UPDATE");
        }
        var e = mapper.selectOne(q);
        if (e == null) {
            throw new ApiRuntimeException("文件不存在或无管理权限");
        }
        return e;
    }

    /**
     * 登记已核验原件；默认状态由实体声明式填充，不伪造解析或向量化成功。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public Long createDocument(KnowledgeDocumentEntity e) {
        mapper.insert(e);
        return e.getId();
    }

    /**
     * 基础资料修改明确支持移动到未分类。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void modifyDocument(KnowledgeDocumentEntity e, Long version) {
        e.setVersion(version);
        int count = mapper.updateById(e);
        if (count != 1) {
            throw new ApiRuntimeException("文件已变化，请刷新重试");
        }
    }

    /**
     * 替换原件保留ID、目录、归属及公开状态，旧处理轮次立即失效。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param expectedVersion 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void replaceDocument(KnowledgeDocumentEntity e, Long expectedVersion) {
        e.setUpdateTime(null);
        e.setUpdateBy(null);
        e.setVersion(expectedVersion);
        // 处理轮次用于使旧解析/向量数据失效，与框架管理的并发version分别维护。
        e.setProcessingVersion(e.getProcessingVersion() + 1);
        e.setProcessingStatus(AiStage.UPLOADED);
        clearProcessingSnapshot(e);
        var update =
                Wrappers.<KnowledgeDocumentEntity>lambdaUpdate()
                        .eq(KnowledgeDocumentEntity::getId, e.getId())
                        .set(KnowledgeDocumentEntity::getProcessingOwner, null)
                        .set(KnowledgeDocumentEntity::getProcessingLeaseUntil, null)
                        .set(KnowledgeDocumentEntity::getProcessingStartedTime, null)
                        .set(KnowledgeDocumentEntity::getParserVersion, null)
                        .set(KnowledgeDocumentEntity::getSplitterVersion, null)
                        .set(KnowledgeDocumentEntity::getChunkMaxTokens, null)
                        .set(KnowledgeDocumentEntity::getChunkOverlapTokens, null)
                        .set(KnowledgeDocumentEntity::getErrorMessage, null)
                        .set(KnowledgeDocumentEntity::getProcessedTime, null);
        int changed = mapper.update(e, update);
        if (changed != 1) {
            throw new ApiRuntimeException("文件替换期间版本变化");
        }
    }

    /** 同步清空内存中的旧处理快照；默认非空更新策略使实体不再生成这些列的赋值， 数据库清空统一由替换更新的Lambda Wrapper负责，避免同一列重复出现在SET中。 */
    private void clearProcessingSnapshot(KnowledgeDocumentEntity document) {
        document.setProcessingOwner(null);
        document.setProcessingLeaseUntil(null);
        document.setProcessingStartedTime(null);
        document.setParserVersion(null);
        document.setSplitterVersion(null);
        document.setChunkMaxTokens(null);
        document.setChunkOverlapTokens(null);
        document.setErrorMessage(null);
        document.setProcessedTime(null);
    }

    /**
     * 先删除投影再删除分片，满足复合外键并避免旧内容继续检索。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void removeProjectionList(Long id) {
        mapper.deleteVector768(id);
        mapper.deleteVector1024(id);
        mapper.deleteVector1536(id);
        mapper.deleteChunkList(id);
    }

    /**
     * 文件逻辑删除，原件物理清理由业务事务写入队列。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void removeDocument(Long id) {
        mapper.deleteById(id);
    }
}
