package org.etd.framework.common.core.model;


import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/8/31
 */
@Data
public class PageModel<T> {


    /**
     * 查询数据列表
     */
    protected List<T> records = Collections.emptyList();

    /**
     * 总数
     */
    protected Integer total = 0;
    /**
     * 每页显示条数，默认 10
     */
    protected Integer size = 10;

    /**
     * 当前页
     */
    protected Integer current = 1;
    /**
     * 总页数
     */
    protected Integer pages;


}
