package org.etd.ai.agent.controller.vo;

import java.time.Instant;
import lombok.Data;

/** Agent卡片和可见基本详情，不暴露系统提示词、模型连接及内部配置。 */
@Data
public class AgentVO {
    /** Agent ID。 */
    private Long id;
    /** 所属组织ID。 */
    private Long orgId;
    /** 名称。 */
    private String name;
    /** 简介。 */
    private String description;
    /** 头像资源路径。 */
    private String avatarPath;
    /** 是否租户内公开使用。 */
    private Boolean isPublic;
    /** 启停状态：0禁用、1启用。 */
    private Integer dataStatus;
    /** 当前乐观锁版本，写操作须回传。 */
    private Long version;
    /** 是否已发布。 */
    private Boolean published;
    /** 是否存在尚未发布的草稿变更，仅描述配置差异。 */
    private Boolean unpublishedChanges;
    /** 数据范围是否允许维护，页面还须结合菜单写权限。 */
    private Boolean writable;
    /** 最近发布时间点。 */
    private Instant publishedTime;
    /** 创建时间点。 */
    private Instant createTime;
    /** 最后修改时间点。 */
    private Instant updateTime;
}
