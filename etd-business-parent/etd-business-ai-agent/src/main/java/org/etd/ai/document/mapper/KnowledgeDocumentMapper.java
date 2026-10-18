package org.etd.ai.document.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.Param;
import org.etd.ai.document.entity.KnowledgeDocumentEntity;

/** 文档业务查询；读取权限由Service注解声明，框架在分页前追加SQL条件。 */
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {
    /**
     * 分页获取当前用户可读文件，folderId为空时由uncategorized决定是否仅查未分类。
     *
     * @param page 受接口页码和容量约束的分页对象
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param folderId 目录标识，空表示未分类或全库统计，具体见方法用途
     * @param uncategorized 是否仅查询未分类文件，不能与目录筛选并用
     * @param keyword 可选名称关键字，不作为SQL片段执行
     * @param dataStatus 可选文档状态，不传时不过滤状态
     * @return 符合当前租户及查询条件的分页结果
     */
    IPage<KnowledgeDocumentEntity> selectReadablePage(
            Page<KnowledgeDocumentEntity> page,
            @Param("baseId") Long baseId,
            @Param("folderId") Long folderId,
            @Param("uncategorized") boolean uncategorized,
            @Param("keyword") String keyword,
            @Param("dataStatus") Integer dataStatus);

    /**
     * 读取单文件前校验当前真实权限，不能只凭文件ID生成下载链接。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @return 通过本方法校验或生成的业务结果
     */
    KnowledgeDocumentEntity selectReadableById(@Param("id") Long id, @Param("baseId") Long baseId);

    /**
     * 删除或替换时先清理三个已部署维度的投影，再清理业务分片。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过本方法校验或生成的业务结果
     */
    @InterceptorIgnore(dataPermission = "true")
    int deleteVector768(@Param("id") Long id);

    /**
     * 清理1024维投影，调用前持有文档行锁。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过本方法校验或生成的业务结果
     */
    @InterceptorIgnore(dataPermission = "true")
    int deleteVector1024(@Param("id") Long id);

    /**
     * 清理1536维投影，调用前持有文档行锁。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过本方法校验或生成的业务结果
     */
    @InterceptorIgnore(dataPermission = "true")
    int deleteVector1536(@Param("id") Long id);

    /**
     * 物理移除旧轮次分片，不保留不存在的历史版本兼容链。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过本方法校验或生成的业务结果
     */
    @InterceptorIgnore(dataPermission = "true")
    int deleteChunkList(@Param("id") Long id);
}
