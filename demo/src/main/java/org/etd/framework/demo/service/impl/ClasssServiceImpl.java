package org.etd.framework.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.common.core.model.PageModel;
import org.etd.framework.demo.entity.Classs;
import org.etd.framework.demo.mapper.ClasssDao;
import org.etd.framework.demo.service.ClasssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * (Classs)表服务实现类
 *
 * @author 牛昌
 * @since 2022-09-28 09:55:53
 */
@Service
public class ClasssServiceImpl implements ClasssService<Classs> {

    @Autowired
    private ClasssDao classsDao;

    @Override
    public IPage<Classs> pageAll(IPage<Classs> pageModel) {
        return classsDao.selectPage(pageModel,new LambdaQueryWrapper<>());
    }
}

