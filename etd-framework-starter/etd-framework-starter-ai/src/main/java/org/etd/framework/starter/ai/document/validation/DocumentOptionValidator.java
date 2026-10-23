package org.etd.framework.starter.ai.document.validation;

import org.etd.framework.starter.ai.document.constant.DocumentOptionLimits;
import org.etd.framework.starter.ai.document.model.DocumentSourceLocation;
import org.springframework.util.Assert;

/** 解析与分片参数共用的范围校验，不改变未配置字段的默认语义。 */
public final class DocumentOptionValidator {
    private DocumentOptionValidator() {}

    /** @param value 可空Token数 @param name 参数中文名称 @throws IllegalArgumentException 非正数 */
    public static void requireTokenCount(Integer value, String name) {
        Assert.isTrue(value == null || value >= DocumentOptionLimits.MIN_TOKEN_COUNT, name + "必须大于零");
    }

    /** @param value 可空比例 @param name 参数中文名称 @throws IllegalArgumentException 非有限数或越界 */
    public static void requireRatio(Double value, String name) {
        Assert.isTrue(value == null || Double.isFinite(value)
                && value >= DocumentOptionLimits.MIN_RATIO && value <= DocumentOptionLimits.MAX_RATIO,
                name + "必须在0至1之间");
    }

    /** @param value 可空正数 @param name 参数中文名称 @throws IllegalArgumentException 非有限数或非正数 */
    public static void requirePositive(Double value, String name) {
        Assert.isTrue(value == null || Double.isFinite(value) && value > 0, name + "必须为有限正数");
    }

    /** @param value 可空标识 @param name 参数中文名称 @throws IllegalArgumentException 显式空白 */
    public static void requireTextWhenPresent(String value, String name) {
        if (value != null) {
            Assert.hasText(value, name + "不能为空白");
        }
    }

    /** @param firstPage 起始页 @param lastPage 结束页 @throws IllegalArgumentException 页码缺少配对或范围错误 */
    public static void requirePageRange(Integer firstPage, Integer lastPage) {
        Assert.isTrue((firstPage == null) == (lastPage == null), "起止页码必须同时提供");
        if (firstPage != null) {
            Assert.isTrue(firstPage >= DocumentSourceLocation.FIRST_PAGE_NUMBER && lastPage >= firstPage,
                    "页码范围无效");
        }
    }
}
