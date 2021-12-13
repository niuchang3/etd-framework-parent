package org.etd.framework.starter.mybaits.annotation;


import java.lang.annotation.*;

/**
 * 数据快照注解
 * update时候必须使用updateById(T )
 * <p>
 * 目前支持：
 * 1、save
 * 2、MyBatis提供的 updateById方法
 * <p>
 * 目前暂不支持：
 * 1、自定义update语句
 * 2、自定义sql语句
 * 3、MyBatis提供的update方法
 *
 * @author 牛昌
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSnapshot {
    /**
     * 快照映射的Class
     *
     * @return
     */
    Class<?> snapshotClass();

    /**
     * 物理主键ID
     *
     * @return
     */
    String sequenceField() default "sequenceNbr";

    /**
     * 快照生成时，存放原始对象物理主键的字段
     *
     * @return
     */
    String originalSequenceField() default "originalSequence";
}
