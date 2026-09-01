package org.etd.framework.starter.mybaits.core;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

import java.time.Instant;


@Data
public class BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @TableFieldExtend("T(java.time.Instant).now()")
    private Instant createTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.framework.common.core.context.model.RequestContext).getUser()?.id")
    private Long createBy;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @TableFieldExtend("T(java.time.Instant).now()")
    private Instant updateTime;

    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @TableFieldExtend("T(org.etd.framework.common.core.context.model.RequestContext).getUser()?.id")
    private Long updateBy;

    /**
     * 数据状态
     */
    @TableField(value = "data_status", fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.framework.common.core.constants.BasicConstant$DataStatus).ENABLED.getCode()")
    private Integer dataStatus;

    /**
     * 租户标识
     */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.framework.common.core.context.model.RequestContext).getTenantCode()")
    private Long tenantId;

    /**
     * 逻辑删除标识：0 未删除，1 已删除
     */
    @TableLogic(value = "0", delval = "1")
    @TableField("del_flag")
    private Integer delFlag;
}
