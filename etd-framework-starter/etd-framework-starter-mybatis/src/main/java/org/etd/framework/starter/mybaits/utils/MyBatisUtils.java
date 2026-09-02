package org.etd.framework.starter.mybaits.utils;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * MyBatis Plus 拦截器构建辅助工具类。
 */
public class MyBatisUtils {

    private MyBatisUtils() {
    }

    /**
     * 向 MyBatis Plus 拦截器链的指定位置插入内部拦截器。
     *
     * @param interceptor 目标 MyBatis Plus 拦截器
     * @param inner       待插入的内部拦截器
     * @param index       插入位置索引
     */
    /**
     * 添加 Interceptor
     *
     * @param interceptor 参数 interceptor
     * @param inner 参数 inner
     * @param index 参数 index
     * @return 处理结果
     */
    public static void addInterceptor(MybatisPlusInterceptor interceptor, InnerInterceptor inner, int index) {
        List<InnerInterceptor> inners = Lists.newArrayList(interceptor.getInterceptors());
        inners.add(index, inner);
        interceptor.setInterceptors(inners);
    }
}
