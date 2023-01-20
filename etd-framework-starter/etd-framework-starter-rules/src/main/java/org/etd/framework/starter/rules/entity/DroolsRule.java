package org.etd.framework.starter.rules.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("drools_rule")
@Data
public class DroolsRule implements Serializable {

    /**
     * 规则id
     */
    @TableId(type = IdType.ASSIGN_ID, value = "ID")
    private String id;
    /**
     * kbase的名字
     */
    @TableField(value = "KIE_BASE_NAME")
    private String kieBaseName;
    /**
     * kiePackageName 包名
     */
    @TableField(value = "KIE_PACKAGE_NAME")
    private String kiePackageName;
    /**
     * 规则内容
     */
    @TableField(value = "RULE_CONTENT")
    private String ruleContent;
    /**
     * 描述
     */
    @TableField(value = "DESCRIPTION")
    private String description;

    /**
     * 规则创建时间
     */
    @TableField(value = "CREATED_TIME")
    private Date createdTime;


    public void validate() {
        if (isBlank(id) || isBlank(this.kieBaseName) || isBlank(this.kiePackageName) || isBlank(this.ruleContent)) {
            throw new RuntimeException("Drools参数有误");
        }
        this.id = this.id.trim();
        this.kieBaseName = this.kieBaseName.trim();
        this.kiePackageName = this.kiePackageName.trim();
        this.ruleContent = this.ruleContent.trim();
    }

    private boolean isBlank(String str) {
        return str == null || str.isEmpty();
    }
}
