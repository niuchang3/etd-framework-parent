package org.etd.framework.starter.ai.document.docling.converter;

import ai.docling.core.DoclingDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.etd.framework.starter.ai.document.constant.DocumentBlockType;
import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.framework.starter.ai.document.model.DocumentBlock;
import org.etd.framework.starter.ai.document.model.DocumentTable;
import org.etd.framework.starter.ai.document.model.DocumentTableCell;
import org.etd.framework.starter.ai.document.model.DocumentSourceLocation;

/** 解释 Docling 正文引用树；不按 texts/tables 数组拼接，避免破坏阅读顺序。 */
public final class DoclingStructureReader {
    /** Docling 结构字段及标签，仅在协议边界解释。 */
    private static final String BODY = "body", CHILDREN = "children", REF = "$ref", PARENT = "parent";
    /** 内容块字段。 */
    private static final String LABEL = "label", TEXT = "text", LEVEL = "level", DATA = "data", PROV = "prov";
    /** 来源页面与矩形字段。 */
    private static final String PAGE = "page_no", PAGES = "pages", SIZE = "size", WIDTH = "width", HEIGHT = "height";
    /** 矩形采用 Docling 的原始页面单位。 */
    private static final String BBOX = "bbox", LEFT = "l", TOP = "t", RIGHT = "r", BOTTOM = "b", ORIGIN = "coord_origin";
    /** 坐标原点代码。 */
    private static final String TOP_LEFT = "TOPLEFT", BOTTOM_LEFT = "BOTTOMLEFT";
    /** 表格行列与单元格字段，偏移区间为零基、左闭右开。 */
    private static final String ROWS = "num_rows", COLUMNS = "num_cols", CELLS = "table_cells";
    /** 单元格起止偏移与表头标记。 */
    private static final String FIRST_ROW = "start_row_offset_idx", END_ROW = "end_row_offset_idx";
    /** 列偏移区间。 */
    private static final String FIRST_COLUMN = "start_col_offset_idx", END_COLUMN = "end_col_offset_idx";
    /** 表头标记。 */
    private static final String COLUMN_HEADER = "column_header", ROW_HEADER = "row_header";
    /** 分组节点没有正文内容。 */
    private static final String GROUP_PREFIX = "#/groups/";
    /** 块类型标签。 */
    private static final String TITLE = "title", SECTION_HEADER = "section_header", TABLE = "table", PICTURE = "picture";
    /** 其他可结构化的正文标签。 */
    private static final String LIST_ITEM = "list_item", CODE = "code", FORMULA = "formula";
    /** 标准段落标签。 */
    private static final String PARAGRAPH = "paragraph";
    /** 图片或表格说明文字标签。 */
    private static final String CAPTION = "caption";
    /** 脚注标签。 */
    private static final String FOOTNOTE = "footnote";
    /** 页眉标签。 */
    private static final String PAGE_HEADER = "page_header";
    /** 页脚标签。 */
    private static final String PAGE_FOOTER = "page_footer";
    /** 文档内部 JSON Pointer 引用以片段标识开头。 */
    private static final String INTERNAL_REFERENCE_PREFIX = "#/";
    /** JSON Pointer 解析前需要移除的 URI 片段标识。 */
    private static final String REFERENCE_FRAGMENT_MARKER = "#";
    /** 作为普通文本输出的标签。 */
    private static final Set<String> TEXT_LABELS = Set.of(TEXT, PARAGRAPH, CAPTION, FOOTNOTE, PAGE_HEADER, PAGE_FOOTER);
    /** 用于 SDK 数据结构到 JSON 树的协议序列化，不加载业务自定义模块。 */
    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * @param source SDK 结构化文档；失败结果可以为空
     * @return 阅读顺序内容及原始文档
     * @throws DocumentParseException 引用结构不完整、重复或循环
     */
    public DoclingContent read(DoclingDocument source) {
        if (source == null) {
            List<DocumentBlock> empty = List.<DocumentBlock>of();
            return new DoclingContent(null, empty);
        }
        JsonNode document = mapper.valueToTree(source);
        JsonNode body = document.path(BODY);
        if (!body.isObject()) {
            throw invalidStructure();
        }
        List<DocumentBlock> blocks = readBody(document, body);
        return new DoclingContent(document, blocks);
    }

    /** 按正文引用树做深度优先遍历，分组节点只决定顺序，不作为正文块输出。 */
    private List<DocumentBlock> readBody(JsonNode document, JsonNode body) {
        ArrayDeque<String> pendingReferences = new ArrayDeque<>();
        HashSet<String> visitedReferences = new HashSet<>();
        ArrayList<DocumentBlock> result = new ArrayList<DocumentBlock>();
        pushChildReferences(body, pendingReferences);
        while (!pendingReferences.isEmpty()) {
            String reference = pendingReferences.pop();
            JsonNode node = resolveReference(document, reference, visitedReferences);
            if (!reference.startsWith(GROUP_PREFIX)) {
                DocumentBlock block = readBlock(document, node, reference);
                result.add(block);
            }
            pushChildReferences(node, pendingReferences);
        }
        return List.copyOf(result);
    }

    /** 栈后进先出，因此子节点必须倒序压入，才能按原数组顺序读取；子节点先于后续兄弟节点处理。 */
    private void pushChildReferences(JsonNode node, ArrayDeque<String> pendingReferences) {
        JsonNode children = node.path(CHILDREN);
        for (int index = children.size() - 1; index >= 0; index--) {
            String reference = children.path(index).path(REF).asText();
            pendingReferences.push(reference);
        }
    }

    /** 引用必须指向文档内对象，重复或循环引用统一拒绝，避免正文重复或遍历无法结束。 */
    private JsonNode resolveReference(JsonNode document, String reference, Set<String> visited) {
        if (!reference.startsWith(INTERNAL_REFERENCE_PREFIX) || !visited.add(reference)) {
            throw invalidStructure();
        }
        JsonNode node = document.at(reference.substring(REFERENCE_FRAGMENT_MARKER.length()));
        if (!node.isObject()) {
            throw invalidStructure();
        }
        return node;
    }

    private DocumentBlock readBlock(JsonNode document, JsonNode node, String reference) {
        DocumentBlockType type = readType(node.path(LABEL).asText());
        String text = node.path(TEXT).isTextual() ? node.path(TEXT).asText() : null;
        String parent = node.path(PARENT).path(REF).asText(null);
        Integer level = type == DocumentBlockType.HEADING && node.path(LEVEL).isInt()
                ? node.path(LEVEL).intValue() : null;
        DocumentTable tableData = type == DocumentBlockType.TABLE ? readTable(node.path(DATA)) : null;
        List<DocumentSourceLocation> locations = readLocations(document, node.path(PROV));
        return new DocumentBlock(reference, parent, type, text, level, locations, tableData);
    }

    private DocumentTable readTable(JsonNode data) {
        ArrayList<DocumentTableCell> cells = new ArrayList<DocumentTableCell>();
        for (JsonNode cell : data.path(CELLS)) {
            String text = cell.path(TEXT).asText(null);
            Integer firstRow = integer(cell.path(FIRST_ROW));
            Integer endRow = integer(cell.path(END_ROW));
            Integer firstColumn = integer(cell.path(FIRST_COLUMN));
            Integer endColumn = integer(cell.path(END_COLUMN));
            boolean columnHeader = cell.path(COLUMN_HEADER).asBoolean();
            boolean rowHeader = cell.path(ROW_HEADER).asBoolean();
            DocumentTableCell parsed = new DocumentTableCell(text, firstRow, endRow, firstColumn, endColumn, columnHeader, rowHeader);
            cells.add(parsed);
        }
        Integer rows = integer(data.path(ROWS));
        Integer columns = integer(data.path(COLUMNS));
        return new DocumentTable(rows, columns, cells);
    }

    private Integer integer(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
    }

    private DocumentBlockType readType(String label) {
        if (TEXT_LABELS.contains(label)) {
            return DocumentBlockType.TEXT;
        }
        return switch (label) {
            case TITLE, SECTION_HEADER -> DocumentBlockType.HEADING;
            case TABLE -> DocumentBlockType.TABLE;
            case PICTURE -> DocumentBlockType.PICTURE;
            case LIST_ITEM -> DocumentBlockType.LIST_ITEM;
            case CODE -> DocumentBlockType.CODE;
            case FORMULA -> DocumentBlockType.FORMULA;
            default -> DocumentBlockType.OTHER;
        };
    }

    private List<DocumentSourceLocation> readLocations(JsonNode document, JsonNode provenance) {
        ArrayList<DocumentSourceLocation> result = new ArrayList<DocumentSourceLocation>();
        for (JsonNode location : provenance) {
            int page = location.path(PAGE).asInt();
            if (page < DocumentSourceLocation.FIRST_PAGE_NUMBER) {
                throw invalidStructure();
            }
            JsonNode size = document.path(PAGES).path(Integer.toString(page)).path(SIZE);
            DocumentSourceLocation source = readRectangle(location, size, page);
            result.add(source);
        }
        return result;
    }

    /** 先判断页面尺寸、矩形字段和坐标原点，再统一转换为左上原点；不完整定位只保留原有页码信息。 */
    private DocumentSourceLocation readRectangle(JsonNode source, JsonNode size, int page) {
        Double width = number(size.path(WIDTH));
        Double height = number(size.path(HEIGHT));
        JsonNode box = source.path(BBOX);
        Double left = number(box.path(LEFT));
        Double right = number(box.path(RIGHT));
        Double top = number(box.path(TOP));
        Double bottom = number(box.path(BOTTOM));
        String origin = box.path(ORIGIN).asText();
        boolean pageSizeAvailable = width != null && height != null && width > 0 && height > 0;
        boolean rectangleAvailable = left != null && right != null && top != null && bottom != null;
        boolean originSupported = TOP_LEFT.equals(origin) || BOTTOM_LEFT.equals(origin);
        if (!pageSizeAvailable || !rectangleAvailable || !originSupported) {
            return new DocumentSourceLocation(page, null, null, null, null, width, height);
        }
        if (BOTTOM_LEFT.equals(origin)) {
            // 左下原点的纵坐标向上增大；转换后向下增大，横坐标保持原值。
            top = height - top;
            bottom = height - bottom;
        }
        if (right < left || bottom < top) {
            throw invalidStructure();
        }
        return new DocumentSourceLocation(page, left, top, right, bottom, width, height);
    }

    private Double number(JsonNode node) {
        return node.isNumber() && Double.isFinite(node.doubleValue()) ? node.doubleValue() : null;
    }

    private DocumentParseException invalidStructure() {
        return new DocumentParseException("解析引擎返回无效的文档引用或定位结构", DocumentParseException.NO_HTTP_STATUS, null);
    }
}
