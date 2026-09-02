package org.etd.framework.starter.mybaits.core;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/**
 * 具有独立组织归属的业务实体基类，对应业务表需包含 org_id 字段。
 * 公共系统表继续使用 BaseEntity，明细表可通过业务主表继承组织权限。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrgScopedEntity extends BaseEntity {

    /**
     * 数据归属组织：新增且未赋值时默认使用当前用户的主组织，更新时不自动填充。
     * 显式指定组织的授权校验、缺少主组织时的必填校验由业务层负责，避免错误归属。
     */
    @TableField(value = "org_id", fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.framework.common.core.context.model.RequestContext).getUser()?.orgId")
    private Long orgId;
}
