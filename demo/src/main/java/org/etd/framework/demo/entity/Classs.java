package org.etd.framework.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.etd.framework.starter.mybaits.annotation.DataSnapshot;

/**
 * (Classs)表实体类
 *
 * @author 牛昌
 * @since 2022-09-28 09:55:53
 */
@DataSnapshot(snapshotClass = ClassSnap.class, originalSequenceField = "old")
@TableName("classs")
@Data
public class Classs {



    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField
    private String name;

}

