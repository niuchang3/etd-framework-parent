package org.etd.framework.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * (Classs)表服务接口
 *
 * @author 牛昌
 * @since 2022-09-28 09:55:53
 */
public interface ClasssService<T> {

    IPage<T> pageAll(IPage<T> pageModel);
}

