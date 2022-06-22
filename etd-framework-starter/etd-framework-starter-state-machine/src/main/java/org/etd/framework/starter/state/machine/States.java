package org.etd.framework.starter.state.machine;

import lombok.Data;

import java.io.Serializable;

/**
 * 状态
 *
 * @param <P> 主键ID
 * @author Administrator
 */
@Data
public class States<P extends Serializable, S extends Serializable> {
    /**
     * 状态ID 注册
     */
    protected P id;

    /**
     * 状态Code
     */
    protected String statusCode;
    /**
     * 状态名称
     */
    protected String statusName;
    /**
     * 业务分组
     */
    protected String businessGroup;
    /**
     * 序号
     */
    protected Integer sort;

}
