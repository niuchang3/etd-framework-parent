package org.etd.framework.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * (ClassSnap)表实体类
 *
 * @author 牛昌
 * @since 2022-09-28 09:55:51
 */
@Data
public class ClassSnap {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField
    private String name;

    @TableField
    private Long old;

}

