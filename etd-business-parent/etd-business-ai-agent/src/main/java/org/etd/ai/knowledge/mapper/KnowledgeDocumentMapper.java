package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;

/** 资料及上传主记录的数据访问。 */
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {
    /** 后台清理专用入口，跨租户读取到期上传并加行锁，不向Controller暴露。
     * @return 小批量过期上传记录，跳过其他实例持有的锁 */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    List<KnowledgeDocumentEntity> selectExpiredUploadList();
    /** @param id 已加行锁的过期资料ID @return 更新行数；不从客户端接收ID */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    int updateExpiredUpload(@Param("id") Long id);
}
