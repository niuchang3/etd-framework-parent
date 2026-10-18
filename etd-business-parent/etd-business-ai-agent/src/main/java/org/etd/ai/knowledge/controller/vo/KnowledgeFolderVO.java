package org.etd.ai.knowledge.controller.vo;

import lombok.Data;

import java.time.Instant;

/** KnowledgeFolderVO 页面响应；所有业务ID按框架Long序列化约定传输。 */
@Data
public class KnowledgeFolderVO {
    /** 雪花业务标识。 */
    private Long id;

    /** 启停状态：0停用、1启用，对应全局BasicConstant.DataStatus。 */
    private Integer dataStatus;

    /** 更新需要回传的乐观锁版本。 */
    private Long version;

    /** 创建时间点。 */
    private Instant createTime;

    /** 更新时间点。 */
    private Instant updateTime;

    /** 父目录标识，根目录为空。 */
    private Long parentId;

    /** 目录名称。 */
    private String folderName;

    /** 排序值。 */
    private Integer sortOrder;

    /** 是否租户内公开。 */
    private Boolean isPublic;

    /** 仅用于到达可读子项的祖先，不允许据此查看隐藏内容。 */
    private Boolean navigationOnly;

    /** 角色数据范围是否允许管理；操作同时要求接口write权限。 */
    private Boolean writable;
}
