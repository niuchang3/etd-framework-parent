package org.etd.framework.starter.ai.document.constant;

/** 文档参数的共享校验边界，不包含部署实例的容量和超时配置。 */
public final class DocumentOptionLimits {
    /** Token数量至少为1，零不代表无限制。 */
    public static final int MIN_TOKEN_COUNT = 1;
    /** 标题推断的最小层级。 */
    public static final int MIN_HEADING_LEVEL = 1;
    /** Docling标题层级推断支持的最大层级。 */
    public static final int MAX_HEADING_LEVEL = 100;
    /** 概率、面积占比及匹配容差的下界。 */
    public static final double MIN_RATIO = 0.0;
    /** 概率、面积占比及匹配容差的上界。 */
    public static final double MAX_RATIO = 1.0;

    private DocumentOptionLimits() {}
}
