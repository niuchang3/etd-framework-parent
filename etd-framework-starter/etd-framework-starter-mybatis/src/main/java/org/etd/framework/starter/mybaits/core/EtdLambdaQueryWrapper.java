package org.etd.framework.starter.mybaits.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.util.ObjectUtils;

import java.util.Collection;

/**
 * 增强型 LambdaQueryWrapper。
 * <p>
 * 自动对传入参数执行 null、空字符串及空集合判空处理。
 * 当传入值为空时自动忽略该查询条件，避免手写繁琐的 condition 判空判断。
 *
 * @param <T> 实体泛型类型
 */
public class EtdLambdaQueryWrapper<T> extends LambdaQueryWrapper<T> {


    /**
     * eq
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> eq(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.eq(column, val);
    }

    /**
     * eq
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> eq(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.eq(condition, column, val);
    }

    /**
     * ne
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> ne(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ne(column, val);
    }

    /**
     * ne
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> ne(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ne(condition, column, val);
    }

    /**
     * le
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> le(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.le(column, val);
    }

    /**
     * le
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> le(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.le(condition, column, val);
    }

    /**
     * ge
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> ge(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ge(column, val);
    }

    /**
     * ge
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> ge(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ge(condition, column, val);
    }


    /**
     * lt
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> lt(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.lt(column, val);
    }

    /**
     * lt
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> lt(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.lt(condition, column, val);
    }

    /**
     * gt
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> gt(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.gt(column, val);
    }

    /**
     * gt
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> gt(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.gt(condition, column, val);
    }

    /**
     * 模糊查询
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> like(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.like(column, val);
    }

    /**
     * 模糊查询
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> like(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.like(condition, column, val);
    }

    /**
     * 模糊查询 Left
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> likeLeft(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeLeft(column, val);
    }

    /**
     * 模糊查询 Left
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> likeLeft(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeLeft(condition, column, val);
    }


    /**
     * 模糊查询 Right
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> likeRight(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeRight(column, val);
    }

    /**
     * 模糊查询 Right
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val 参数 val
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> likeRight(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeRight(condition, column, val);
    }

    /**
     * between
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val1 参数 val1
     * @param val2 参数 val2
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> between(SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.between(column, val1, val2);
        }
        return this;
    }

    /**
     * between
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val1 参数 val1
     * @param val2 参数 val2
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> between(boolean condition, SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.between(condition, column, val1, val2);
        }
        return this;
    }

    /**
     * not Between
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val1 参数 val1
     * @param val2 参数 val2
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> notBetween(SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.notBetween(column, val1, val2);
        }
        return this;
    }

    /**
     * not Between
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param val1 参数 val1
     * @param val2 参数 val2
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> notBetween(boolean condition, SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.notBetween(condition, column, val1, val2);
        }
        return this;
    }

    /**
     * in
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param values 参数 values
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> in(SFunction<T, ?> column, Object... values) {
        return ObjectUtils.isEmpty(values) ? this : (EtdLambdaQueryWrapper<T>) super.in(column, values);
    }

    /**
     * in
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param coll 参数 coll
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> in(SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.in(column, coll);
    }

    /**
     * in
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param values 参数 values
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        return ObjectUtils.isEmpty(values) ? this : (EtdLambdaQueryWrapper<T>) super.in(condition, column, values);
    }

    /**
     * in
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param coll 参数 coll
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> in(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.in(condition, column, coll);
    }

    /**
     * not In
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param value 参数 value
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> notIn(SFunction<T, ?> column, Object... value) {
        return ObjectUtils.isEmpty(value) ? this : (EtdLambdaQueryWrapper<T>) super.notIn(column, value);
    }

    /**
     * not In
     *
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param coll 参数 coll
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> notIn(SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.notIn(column, coll);
    }

    /**
     * not In
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param values 参数 values
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        return ObjectUtils.isEmpty(values) ? this : (EtdLambdaQueryWrapper<T>) super.notIn(condition, column, values);
    }

    /**
     * not In
     *
     * @param condition 参数 condition
     * @param SFunction<T 参数 SFunction<T
     * @param column 参数 column
     * @param coll 参数 coll
     * @return 处理结果
     */
    @Override
    public EtdLambdaQueryWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.notIn(condition, column, coll);
    }

}
