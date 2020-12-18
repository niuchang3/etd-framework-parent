package org.etd.framework.starter.mybaits.handler;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.etd.framework.starter.mybaits.annotation.TableFieldExtend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 新增修改时默认填充数据
 * 使用方式：
 *
 * @author Young
 * @description
 * @date 2020/6/16
 */
@Slf4j
@Component
public abstract class MetaObjectHandlerExtend implements MetaObjectHandler {


	@Autowired
	private ApplicationContext applicationContext;

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
			this.strictInsertFill(metaObject, fieldInfo.getField().getName(), fieldInfo.getField().getType(), value);
		}
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		for (TableFieldInfo fieldInfo : findTableInfo(metaObject).getFieldList()) {
			if (!validateTableFieldInfo(fieldInfo) || !validateUpdataFill(fieldInfo)) {
				continue;
			}
			Object value = getFillValu(fieldInfo);
			if (value == null) {
				continue;
			}
			this.strictUpdateFill(metaObject, fieldInfo.getField().getName(), fieldInfo.getField().getType(), value);
		}
	}

	/**
	 * 校验修改SQL注解
	 *
	 * @param fieldInfo
	 * @return
	 */
	private boolean validateUpdataFill(TableFieldInfo fieldInfo) {
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
		ExpressionParser parser = new SpelExpressionParser();
		try {

			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setBeanResolver(new BeanFactoryResolver(applicationContext.getAutowireCapableBeanFactory()));
			TableFieldExtend extend = fieldInfo.getField().getAnnotation(TableFieldExtend.class);
			String expression = extend.value();
			if (StringUtils.isEmpty(expression)) {
				expression = extend.expression();
			}
			if (StringUtils.isEmpty(expression)) {
				return null;
			}
			return parser.parseExpression(expression).getValue(context, fieldInfo.getField().getType());
		} catch (EvaluationException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
