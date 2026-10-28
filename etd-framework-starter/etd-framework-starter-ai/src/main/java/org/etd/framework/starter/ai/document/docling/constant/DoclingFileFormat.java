package org.etd.framework.starter.ai.document.docling.constant;

import java.util.HashSet;
import java.util.Set;

/** 当前适配器支持的格式及 OCR/页码选项能力，避免能力声明和请求校验分别维护。 */
public enum DoclingFileFormat {
    /** PDF 文档。 */
    PDF("pdf", true),
    /** Word 文档。 */
    DOCX("docx", false),
    /** PowerPoint 文档。 */
    PPTX("pptx", false),
    /** Excel 工作簿。 */
    XLSX("xlsx", false),
    /** HTML 文档。 */
    HTML("html", false),
    /** HTM 文档。 */
    HTM("htm", false),
    /** Markdown 文档。 */
    MARKDOWN("md", false),
    /** CSV 表格。 */
    CSV("csv", false),
    /** PNG 图片。 */
    PNG("png", true),
    /** JPG 图片。 */
    JPG("jpg", true),
    /** JPEG 图片。 */
    JPEG("jpeg", true),
    /** TIF 图片。 */
    TIF("tif", true),
    /** TIFF 图片。 */
    TIFF("tiff", true),
    /** BMP 图片。 */
    BMP("bmp", true),
    /** WebP 图片。 */
    WEBP("webp", true);

    /** 所有支持的扩展名，只读且由枚举定义生成。 */
    private static final Set<String> SUPPORTED_EXTENSIONS = collectExtensions();
    /** 小写扩展名，不带点，作为稳定的文件格式代码。 */
    private final String code;
    /** 是否允许显式配置 OCR 和页码范围，仅对 PDF/图片启用。 */
    private final boolean supportsOcrAndPageRange;

    DoclingFileFormat(String code, boolean supportsOcrAndPageRange) {
        this.code = code;
        this.supportsOcrAndPageRange = supportsOcrAndPageRange;
    }

    /** @return 小写文件扩展名 */
    public String getCode() {
        return code;
    }

    /** @return 是否允许显式配置 OCR 和页码范围 */
    public boolean supportsOcrAndPageRange() {
        return supportsOcrAndPageRange;
    }

    /** @return 当前支持的扩展名集合，不可修改 */
    public static Set<String> supportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    /**
     * @param extension 已规范为小写且不带点的扩展名
     * @return 对应格式及选项能力
     * @throws IllegalArgumentException 当前适配器不支持该扩展名
     */
    public static DoclingFileFormat fromExtension(String extension) {
        for (DoclingFileFormat format : values()) {
            if (format.code.equals(extension)) {
                return format;
            }
        }
        throw new IllegalArgumentException("当前解析适配器不支持该文件格式");
    }

    private static Set<String> collectExtensions() {
        HashSet<String> extensions = new HashSet<String>();
        for (DoclingFileFormat format : values()) {
            extensions.add(format.code);
        }
        return Set.copyOf(extensions);
    }
}
