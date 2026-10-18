package org.etd.ai.upload.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;
import org.etd.ai.upload.entity.UploadSessionEntity;

import java.util.List;

/** 上传会话持久化；维护扫描只由内部定时任务调用，不暴露HTTP入口。 */
public interface UploadSessionMapper extends BaseMapper<UploadSessionEntity> {
    /**
     * 系统按过期时间分批扫描，锁定记录避免多个实例重复取消。
     *
     * @return 符合条件的记录集合，不在Java中拼接跨表权限数据
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    List<UploadSessionEntity> selectExpiredSessionList();

    /**
     * 系统维护按全局任务ID取消，HTTP入口不可调用。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过本方法校验或生成的业务结果
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    int updateExpiredSession(@Param("id") Long id);
}
