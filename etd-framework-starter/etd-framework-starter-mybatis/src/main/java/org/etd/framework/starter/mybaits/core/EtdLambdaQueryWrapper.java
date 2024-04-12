package org.etd.framework.starter.mybaits.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.util.ObjectUtils;

import java.util.Collection;

/**
 * 重写LambdaQueryWrapper 做判空处理
 *
 * @param <T>
 */
public class EtdLambdaQueryWrapper<T> extends LambdaQueryWrapper<T> {


    @Override
    public EtdLambdaQueryWrapper<T> eq(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.eq(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> eq(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.eq(condition, column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> ne(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ne(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> ne(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ne(condition, column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> le(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.le(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> le(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.le(condition, column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> ge(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ge(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> ge(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.ge(condition, column, val);
    }


    @Override
    public EtdLambdaQueryWrapper<T> lt(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.lt(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> lt(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.lt(condition, column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> gt(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.gt(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> gt(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.gt(condition, column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> like(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.like(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> like(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.like(condition, column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> likeLeft(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeLeft(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> likeLeft(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeLeft(condition, column, val);
    }


    @Override
    public EtdLambdaQueryWrapper<T> likeRight(SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeRight(column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> likeRight(boolean condition, SFunction<T, ?> column, Object val) {
        return ObjectUtils.isEmpty(val) ? this : (EtdLambdaQueryWrapper<T>) super.likeRight(condition, column, val);
    }

    @Override
    public EtdLambdaQueryWrapper<T> between(SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.between(column, val1, val2);
        }
        return this;
    }

    @Override
    public EtdLambdaQueryWrapper<T> between(boolean condition, SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.between(condition, column, val1, val2);
        }
        return this;
    }

    @Override
    public EtdLambdaQueryWrapper<T> notBetween(SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.notBetween(column, val1, val2);
        }
        return this;
    }

    @Override
    public EtdLambdaQueryWrapper<T> notBetween(boolean condition, SFunction<T, ?> column, Object val1, Object val2) {
        if (!ObjectUtils.isEmpty(val1) && !ObjectUtils.isEmpty(val2)) {
            return (EtdLambdaQueryWrapper<T>) super.notBetween(condition, column, val1, val2);
        }
        return this;
    }

    @Override
    public EtdLambdaQueryWrapper<T> in(SFunction<T, ?> column, Object... values) {
        return ObjectUtils.isEmpty(values) ? this : (EtdLambdaQueryWrapper<T>) super.in(column, values);
    }

    @Override
    public EtdLambdaQueryWrapper<T> in(SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.in(column, coll);
    }

    @Override
    public EtdLambdaQueryWrapper<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        return ObjectUtils.isEmpty(values) ? this : (EtdLambdaQueryWrapper<T>) super.in(condition, column, values);
    }

    @Override
    public EtdLambdaQueryWrapper<T> in(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.in(condition, column, coll);
    }

    @Override
    public EtdLambdaQueryWrapper<T> notIn(SFunction<T, ?> column, Object... value) {
        return ObjectUtils.isEmpty(value) ? this : (EtdLambdaQueryWrapper<T>) super.in(column, value);
    }

    @Override
    public EtdLambdaQueryWrapper<T> notIn(SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.in(column, coll);
    }

    @Override
    public EtdLambdaQueryWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        return ObjectUtils.isEmpty(values) ? this : (EtdLambdaQueryWrapper<T>) super.in(condition, column, values);
    }

    @Override
    public EtdLambdaQueryWrapper<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        return ObjectUtils.isEmpty(coll) ? this : (EtdLambdaQueryWrapper<T>) super.in(condition, column, coll);
    }

}
