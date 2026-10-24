package org.etd.ai.knowledge.mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.model.KnowledgeSearchHit;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import java.util.List;
/** 全文及向量候选，统一JOIN实时权限与发布状态。 */
// 继承权限由知识库可信范围SQL/授权入口控制，避免对子表按上传人重复过滤；租户拦截仍启用。
@InterceptorIgnore(dataPermission = "true")
public interface KnowledgeRetrievalMapper {
    /** @param base 当前库 @param query 问题 @param scope 可信范围 @param dataStatus 状态过滤 @return 全文候选 */
    List<KnowledgeSearchHit> selectKeywordList(@Param("base") KnowledgeBaseEntity base, @Param("query") String query,
            @Param("scope") DataPermissionScope scope, @Param("dataStatus") Integer dataStatus);
    /** @param base 库配置 @param table 白名单表名 @param vector 绑定向量文本 @param scope 可信范围 @param dataStatus 状态 @return 向量候选 */
    List<KnowledgeSearchHit> selectVectorList(@Param("base") KnowledgeBaseEntity base, @Param("table") String table,
            @Param("vector") String vector, @Param("scope") DataPermissionScope scope, @Param("dataStatus") Integer dataStatus);
    /** @param base 构建配置 @return 已确认分片快照 */
    List<KnowledgeSearchHit> selectBuildChunkList(@Param("base") KnowledgeBaseEntity base);
    /** @param base 构建配置 @param table 白名单表 @param chunk 分片快照 @return 已保存同代次结果数 */
    long selectExistingVectorCount(@Param("base") KnowledgeBaseEntity base, @Param("table") String table,
            @Param("chunk") KnowledgeSearchHit chunk);
    /** @param table 白名单表 @param id 新向量ID @param content 正文 @param metadata JSON @param vector 向量 */
    // tenant_id是metadata生成列，数据库自动计算；仅内部任务可信载荷写入，禁止拦截器重复赋值。
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    void insertVector(@Param("table") String table, @Param("id") Long id, @Param("content") String content,
            @Param("metadata") String metadata, @Param("vector") String vector);
    /** @param base 构建库 @return 构建期间已确认内容是否存在漏项 */
    long selectMissingCount(@Param("base") KnowledgeBaseEntity base, @Param("table") String table);
    /** @param base 生效库 @param table 白名单表 @param documentId 文档 @return 缺失向量的分片数 */
    long selectDocumentMissingCount(@Param("base") KnowledgeBaseEntity base, @Param("table") String table,
            @Param("documentId") Long documentId);
    /** @param table 白名单表 @param documentId 已授权删除文档，所有代次向量随事务清理 */
    void deleteDocumentVectors(@Param("table") String table, @Param("documentId") Long documentId);
    /** 后台清理过期代次，保留当前生效及正在构建代次，延迟十分钟让在途请求结束。 @param table 白名单物理表 */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    void deleteRetiredVectors(@Param("table") String table);
    /** 后台入口跨租户读取状态，仅用于任务恢复，不向Controller暴露。 @return 待处理构建 */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    List<KnowledgeBaseEntity> selectPendingBuildList();
    /** @param id 内部任务ID @param version 构建版本 @return 租约领取行数 */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    int claimBuild(@Param("id") Long id, @Param("version") Long version);
}
