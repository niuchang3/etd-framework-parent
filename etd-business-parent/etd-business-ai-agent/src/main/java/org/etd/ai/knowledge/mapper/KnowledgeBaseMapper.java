package org.etd.ai.knowledge.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
/** 知识库持久化入口，角色范围及公开读取由框架统一注入。 */
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {
    /** @param page 分页 @param keyword 名称 @return 可见知识库 */
    IPage<KnowledgeBaseEntity> selectReadablePage(IPage<KnowledgeBaseEntity> page, @Param("keyword") String keyword);
    /** @param id 知识库ID @param lock 是否加行锁 @return 可访问记录 */
    KnowledgeBaseEntity selectAccessible(@Param("id") Long id, @Param("lock") boolean lock);
    /** @param id 已授权知识库 @return 未删除资料数量 */
    long selectDocumentCount(@Param("id") Long id);
}
