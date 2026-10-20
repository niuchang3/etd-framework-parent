package org.etd.ai.knowledge.mapper;

import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

/** 固定维度向量写入，正文及租户归属通过绑定参数传递。 */
public interface KnowledgeVectorMapper {
    /** @param id 向量ID @param dimension 服务端校验的维度 @param content 正文 @param metadata JSON元数据 @param vector 向量文本 */
    // 租户字段由可信metadata生成，禁止拦截器向生成列重复追加INSERT值。
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    int insertVector(@Param("id") Long id, @Param("dimension") int dimension,
            @Param("content") String content, @Param("metadata") String metadata, @Param("vector") String vector);
}
