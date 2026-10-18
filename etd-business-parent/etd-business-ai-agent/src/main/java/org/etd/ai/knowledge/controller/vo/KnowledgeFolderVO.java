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

    /** 目录描述，选填，最多500字。 */
    private String description;

    /** 排序值。 */
    private Integer sortOrder;

    /** 是否仅用于导航；租户共享目录始终为false。 */
    private Boolean navigationOnly;

    /** 租户范围内可维护；操作同时要求接口write权限。 */
    private Boolean writable;
}
