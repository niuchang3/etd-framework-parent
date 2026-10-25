package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;

/** 资料及上传主记录的数据访问。 */
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {
    /** @param page 分页 @param knowledgeBaseId 知识库 @param folderId 单目录 @param folderIds 多目录
     * @param keyword 名称 @param uploadStatus 上传状态 @param chunkStatus 分片状态 @param dataStatus 启停 @return 可读文档 */
    IPage<KnowledgeDocumentEntity> selectReadablePage(IPage<KnowledgeDocumentEntity> page,
        @Param("knowledgeBaseId") Long knowledgeBaseId, @Param("folderId") Long folderId,
        @Param("folderIds") List<Long> folderIds, @Param("keyword") String keyword,
        @Param("uploadStatus") String uploadStatus, @Param("chunkStatus") String chunkStatus,
        @Param("dataStatus") Integer dataStatus);
    /** @param id 文档 @param lock 是否锁定 @return 可读文档 */
    KnowledgeDocumentEntity selectAccessible(@Param("id") Long id, @Param("lock") boolean lock);

    /** 后台清理专用入口，跨租户读取到期上传并加行锁，不向Controller暴露。
     * @return 小批量过期上传记录，跳过其他实例持有的锁 */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    List<KnowledgeDocumentEntity> selectExpiredUploadList();
    /** @param id 已加行锁的过期资料ID @return 更新行数；不从客户端接收ID */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    int updateExpiredUpload(@Param("id") Long id);
    /** @param baseId 已授权知识库 @param keyword 是否全文模式 @return 状态已调整的资料数，同时递增版本取消在途确认 */
    int updateVectorStatusByBase(@Param("baseId") Long baseId, @Param("keyword") boolean keyword);
    /** @param id 本次确认资料 @param version 本次processing版本 @return 实际标记失败的行数，0表示请求已经失效 */
    int updateVectorFailure(@Param("id") Long id, @Param("version") Long version);
}
