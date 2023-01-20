package org.etd.framework.starter.rules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@TableName("drools_rule_names")
@Data
public class DroolsRuleNames implements Serializable {

    @TableId(type = IdType.ASSIGN_ID, value = "ID")
    private String id;

    @TableField(value = "DROOLS_RULE_ID")
    private String droolsRuleId;

    @TableField(value = "RULE_NAME")
    private String ruleName;
}
