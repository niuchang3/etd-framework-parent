package org.etd.ai.upload.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;
import org.etd.ai.upload.entity.StorageCleanupEntity;

import java.util.List;

/** 事务内登记、后台可重试的存储清理队列。 */
public interface StorageCleanupMapper extends BaseMapper<StorageCleanupEntity> {
    /**
     * 固定批次跨租户清理，禁止删除仍由当前文件引用的对象。
     *
     * @return 符合条件的记录集合，不在Java中拼接跨表权限数据
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    List<StorageCleanupEntity> selectDueCleanupList();

    /**
     * 按全局任务ID删除已成功清理记录。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过本方法校验或生成的业务结果
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    int deleteCompleted(@Param("id") Long id);

    /**
     * 失败后保留记录并延迟重试，不能吞掉清理失败。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过本方法校验或生成的业务结果
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    int updateRetry(@Param("id") Long id);
}
