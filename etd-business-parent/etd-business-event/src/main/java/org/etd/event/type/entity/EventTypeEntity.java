package org.etd.event.type.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

/**
 * 事件类型定义实体，事件类型编码作为跨应用稳定协议，创建后不得修改或复用。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "evt_event_type", excludeProperty = "tenantId")
public class EventTypeEntity extends BaseEntity {

    /**
     * 数据库记录乐观锁版本。
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * 全局唯一事件类型，例如 {@code upms.user.created}。
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件类型页面显示名称。
     */
    @TableField("event_name")
    private String eventName;

    /**
     * 定义并产生该事件的来源应用。
     */
    @TableField("source_application")
    private String sourceApplication;

    /**
     * 当前登记的最新事件协议版本。
     */
    @TableField("latest_version")
    private Integer latestVersion;

    /**
     * 事件业务语义和使用约束说明。
     */
    @TableField("description")
    private String description;
}
