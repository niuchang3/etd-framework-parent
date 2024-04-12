package org.etd.framework.starter.mybaits.core;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;


@Data
public class BaseEntity {

    /**
     * 租户ID
     */
    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 创建时间
     */
    @TableField("CREATE_TIME")
    private Date createTime;
    /**
     * 数据状态
     */
    @TableField("DATA_STATUS")
    private Boolean dataStatus;
}
