package org.etd.framework.starter.ai.document.model;


/**
 * 原文定位；坐标存在时统一为页面左上角原点、向右和向下递增的页面原始单位。
 * @param page 页码，从 1 开始
 * @param left 左边界；没有完整页面尺寸或有效坐标时为空
 * @param top 上边界，可为空
 * @param right 右边界，可为空
 * @param bottom 下边界，可为空
 * @param pageWidth 页面宽度，可为空
 * @param pageHeight 页面高度，可为空
 */
public record DocumentSourceLocation(
        int page,
        Double left,
        Double top,
        Double right,
        Double bottom,
        Double pageWidth,
        Double pageHeight) {
    /** 公共文档页码从 1 开始，请求范围校验与结果定位使用同一边界。 */
    public static final int FIRST_PAGE_NUMBER = 1;
}
