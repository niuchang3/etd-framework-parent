package org.etd.framework.starter.mybaits.fill.handler;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * 新增修改时默认填充数据
 * 使用方式：
 *
 * @author Young
 * @description
 * @date 2020/6/16
 */
@Slf4j
public abstract class DataFillHandler implements MetaObjectHandler, ApplicationContextAware {

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @SneakyThrows
    @Override
    public void insertFill(MetaObject metaObject) {
        for (TableFieldInfo fieldInfo : findTableInfo(metaObject).getFieldList()) {
            if (!validateTableFieldInfo(fieldInfo) || !validateInsertFill(fieldInfo)) {
                continue;
            }
            Object value = getFillValu(fieldInfo);
            if (value == null) {
                continue;
            }
            fillFieldIfAbsent(fieldInfo, value, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        for (TableFieldInfo fieldInfo : findTableInfo(metaObject).getFieldList()) {
            if (!validateTableFieldInfo(fieldInfo) || !validateUpdateFill(fieldInfo)) {
                continue;
            }
            Object value = getFillValu(fieldInfo);
            if (value == null) {
                continue;
            }
            fillFieldIfAbsent(fieldInfo, value, metaObject);
        }
    }

    /**
     * 仅在字段未赋值时自动填充，避免覆盖调用方显式设置的审计信息。
     */
    private void fillFieldIfAbsent(TableFieldInfo fieldInfo, Object value, MetaObject metaObject) {
        String fieldName = fieldInfo.getField().getName();
        if (getFieldValByName(fieldName, metaObject) != null) {
            return;
        }
        setFieldValByName(fieldName, value, metaObject);
    }

    /**
     * 校验修改SQL注解
     *
     * @param fieldInfo
     * @return
     */
    private boolean validateUpdateFill(TableFieldInfo fieldInfo) {
        TableField field = fieldInfo.getField().getAnnotation(TableField.class);
        return FieldFill.UPDATE.equals(field.fill()) || FieldFill.INSERT_UPDATE.equals(field.fill());
    }

    /**
     * 校验新增SQL注解
     *
     * @param fieldInfo
     * @return
     */
    private boolean validateInsertFill(TableFieldInfo fieldInfo) {
        TableField field = fieldInfo.getField().getAnnotation(TableField.class);
        return FieldFill.INSERT.equals(field.fill()) || FieldFill.INSERT_UPDATE.equals(field.fill());
    }

    /**
     * 校验TableFiel
     *
     * @param fieldInfo
     * @return
     */
    private boolean validateTableFieldInfo(TableFieldInfo fieldInfo) {
        if (!fieldInfo.getField().isAnnotationPresent(TableFieldExtend.class)) {
            return false;
        }
        if (!fieldInfo.getField().isAnnotationPresent(TableField.class)) {
            return false;
        }
        return true;
    }

    /**
     * 解析
     *
     * @param fieldInfo
     */
    private Object getFillValu(TableFieldInfo fieldInfo) {
        String expression = null;
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setBeanResolver(new BeanFactoryResolver(applicationContext.getAutowireCapableBeanFactory()));
            TableFieldExtend extend = fieldInfo.getField().getAnnotation(TableFieldExtend.class);
            expression = extend.value();
            if (StringUtils.isEmpty(expression)) {
                expression = extend.expression();
            }
            if (StringUtils.isEmpty(expression)) {
                return null;
            }
            return EXPRESSION_PARSER.parseExpression(expression).getValue(context, fieldInfo.getField().getType());
        } catch (EvaluationException | ParseException exception) {
            log.warn("自动填充字段解析失败，字段名：{}，表达式：{}",
                    fieldInfo.getField().getName(), expression, exception);
        }
        return null;
    }
}
