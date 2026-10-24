package org.etd.ai.knowledge.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
/** 知识库持久化及继承权限的读取入口。 */
// 继承权限由知识库可信范围SQL/授权入口控制，避免对子表按上传人重复过滤；租户拦截仍启用。
@InterceptorIgnore(dataPermission = "true")
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {
    /** @param page 分页 @param keyword 名称 @param scope 框架可信范围 @return 可见知识库 */
    IPage<KnowledgeBaseEntity> selectReadablePage(IPage<KnowledgeBaseEntity> page, @Param("keyword") String keyword,
            @Param("scope") DataPermissionScope scope);
    /** @param id 知识库ID @param scope 可信范围 @param lock 是否加行锁 @return 可访问记录 */
    KnowledgeBaseEntity selectAccessible(@Param("id") Long id, @Param("scope") DataPermissionScope scope, @Param("lock") boolean lock);
    /** @param id 已授权知识库 @return 未删除资料数量 */
    long selectDocumentCount(@Param("id") Long id);
}
