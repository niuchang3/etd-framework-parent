package org.etd.framework.starter.mybaits.core;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;


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
    @TableField("create_time")
    private Date createTime;
    /**
     * 数据状态
     */
    @TableField("data_status")
    private Boolean dataStatus;
}
