package org.etd.framework.starter.ai.document.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.Builder;
import org.springframework.util.Assert;

/** PDF标题层级推断细项；是否启用统一由外层doPdfHeadingHierarchy控制。 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DocumentHeadingHierarchyOptions(
        /** 是否使用PDF书签推断层级。 */
        Boolean useBookmarks,
        /** 是否使用章节编号推断层级。 */
        Boolean useNumbering,
        /** 是否使用标题样式分组推断层级。 */
        Boolean useStyle,
        /** 是否将字体样式纳入分组。 */
        Boolean useFontStyle,
        /** 字号分组容差，范围0至1。 */
        Double styleSizeTolerance,
        /** 编号规则名称列表，具体支持值由服务定义。 */
        List<String> numberingSchemes,
        /** 标题层级上限，范围1至100。 */
        Integer maxLevel,
        /** 书签匹配阈值，范围0至1。 */
        Double bookmarkMatchThreshold) {

    /** 验证数值范围，保留未指定字段的服务默认值。 */
    public DocumentHeadingHierarchyOptions {
        requireRatio(styleSizeTolerance);
        requireRatio(bookmarkMatchThreshold);
        Assert.isTrue(maxLevel == null || maxLevel >= 1 && maxLevel <= 100, "标题层级上限必须在1至100之间");
        if (numberingSchemes != null) {
            numberingSchemes = List.copyOf(numberingSchemes);
            for (String scheme : numberingSchemes) {
                Assert.hasText(scheme, "编号规则名称不能为空白");
            }
        }
    }

    private static void requireRatio(Double value) {
        Assert.isTrue(value == null || Double.isFinite(value) && value >= 0 && value <= 1,
                "标题匹配阈值和样式容差必须在0至1之间");
    }
}
