package org.etd.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.ai.agent.mapper.typehandler.AgentConfigurationTypeHandler;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/** Agent定义；租户、组织和审计属性复用框架基类，不重复声明。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_agent", autoResultMap = true)
public class AgentEntity extends OrgScopedEntity {
    /** Agent名称，不要求租户内唯一。 */
    private String name;
    /** Agent用途简介，清空通过局部更新显式写入null。 */
    private String description;
    /** 头像资源路径，清空通过局部更新显式写入null。 */
    private String avatarPath;
    /** 是否租户内公开使用，公开读取不授予配置维护权限。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean isPublic;
    /** 非空草稿配置，保存不会修改当前发布配置。 */
    @TableField(typeHandler = AgentConfigurationTypeHandler.class)
    private AgentConfiguration draftConfig;
    /** 当前发布配置，未发布时为空，不提供历史配置回滚。 */
    @TableField(typeHandler = AgentConfigurationTypeHandler.class)
    private AgentConfiguration publishedConfig;
    /** 最近发布时间点。 */
    private Instant publishedTime;
    /** 最近发布用户ID，从可信身份取得。 */
    private Long publishedBy;
    /** 乐观锁版本，新增默认0，更新必须携带页面读取值。 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Long version;
    /** 当前数据权限是否允许维护，不等于菜单写权限。 */
    @TableField(exist = false)
    private Boolean writable;
    /** 是否存在发布配置，由查询计算。 */
    @TableField(exist = false)
    private Boolean published;
    /** 草稿是否与发布配置不同，由JSONB语义比较计算。 */
    @TableField(exist = false)
    private Boolean unpublishedChanges;
}
