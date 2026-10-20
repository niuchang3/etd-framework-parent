package org.etd.ai.knowledge.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.ZipInputStream;

/** 使用现有JDK解析UTF-8文本及DOCX，并按实际Token数生成不破坏Unicode的正文分片。 */
@Service
@RequiredArgsConstructor
public class KnowledgeTextService {
    /** 固定分词器，处理结果记录对应规则版本。 */
    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
    /** 可直接按UTF-8正文读取的文件格式，不是上传白名单。 */
    private static final Set<String> TEXT_FORMATS = Set.of("txt", "md", "csv", "json", "html", "htm", "xml", "log");
    private final KnowledgeUploadProperties properties;

    /** @param input 原件流 @param filename 原始文件名 @return 可切分的正文
     * @throws Exception 非UTF-8文本、格式暂未支持或正文过大 */
    public String parseText(InputStream input, String filename) throws Exception {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String text;
        if ("docx".equals(extension)) {
            text = parseDocx(input);
        } else if (TEXT_FORMATS.contains(extension)) {
            byte[] bytes = readLimited(input);
            var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
            var buffer = ByteBuffer.wrap(bytes);
            text = decoder.decode(buffer).toString();
        } else {
            throw new ApiRuntimeException("文件已上传，当前解析器暂不支持该格式");
        }
        if (text.isBlank() || text.indexOf('\0') >= 0) {
            throw new ApiRuntimeException("原件未包含可解析正文或不是有效文本");
        }
        return text;
    }

    /** 解压仅提取主文档，限制解压体积并关闭DTD和外部实体，避免XXE。 */
    private String parseDocx(InputStream input) throws Exception {
        try (var zip = new ZipInputStream(input)) {
            var entry = zip.getNextEntry();
            long expanded = 0;
            while (entry != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    byte[] xml = readLimited(zip);
                    return extractDocxText(xml);
                }
                // 同样限制被跳过的条目解压体积，防止压缩炸弹。
                expanded += readLimited(zip).length;
                if (expanded > properties.getMaxTextBytes()) {
                    throw new ApiRuntimeException("DOCX累计解压内容超过限制");
                }
                entry = zip.getNextEntry();
            }
        }
        throw new ApiRuntimeException("DOCX缺少主文档正文");
    }

    /** 只读取Word正文的文本节点，保留段落边界。 */
    private String extractDocxText(byte[] xml) throws Exception {
        var factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        var source = new ByteArrayInputStream(xml);
        var reader = factory.createXMLStreamReader(source);
        var text = new StringBuilder();
        boolean inText = false;
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    inText = "t".equals(reader.getLocalName());
                } else if (event == XMLStreamConstants.CHARACTERS && inText) {
                    text.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("p".equals(reader.getLocalName())) {
                        text.append('\n');
                    }
                    inText = false;
                }
            }
        } finally {
            reader.close();
        }
        return text.toString();
    }

    /** 正文读取有明确内存上限，不对完整大文件使用无界readAllBytes。 */
    private byte[] readLimited(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(properties.getMaxTextBytes() + 1);
        if (bytes.length > properties.getMaxTextBytes()) {
            throw new ApiRuntimeException("解析正文超过配置的大小限制");
        }
        return bytes;
    }

    /** @param text 正文 @param maxTokens 分片Token上限 @param overlap 重叠Token数量 @return 有序正文片段 */
    public List<String> splitText(String text, int maxTokens, int overlap) {
        if (maxTokens <= 0 || overlap < 0 || overlap >= maxTokens) {
            throw new ApiRuntimeException("分片参数无效");
        }
        var result = new ArrayList<String>();
        int start = 0;
        while (start < text.length()) {
            int end = findTokenBoundary(text, start, maxTokens);
            String content = text.substring(start, end);
            if (!content.isBlank()) {
                result.add(content);
            }
            if (end == text.length()) {
                break;
            }
            int next = findOverlapStart(text, start, end, overlap);
            start = next > start ? next : end;
        }
        return result;
    }

    /** 在Unicode码点边界内查找满足Token上限的前缀，避免按Token字节切断汉字。 */
    private int findTokenBoundary(String text, int start, int maxTokens) {
        int count = text.codePointCount(start, Math.min(text.length(), start + maxTokens * 16));
        int lower = 1;
        int upper = count;
        int end = start;
        while (lower <= upper) {
            int middle = (lower + upper) >>> 1;
            int boundary = text.offsetByCodePoints(start, middle);
            if (countTokens(text.substring(start, boundary)) <= maxTokens) {
                end = boundary;
                lower = middle + 1;
            } else {
                upper = middle - 1;
            }
        }
        if (end == start) {
            throw new ApiRuntimeException("分片Token上限不足以容纳单个字符");
        }
        return end;
    }

    /** 从片尾保留不超过目标Token数的完整Unicode后缀。 */
    private int findOverlapStart(String text, int start, int end, int overlap) {
        int result = end;
        while (result > start && overlap > 0) {
            int previous = text.offsetByCodePoints(result, -1);
            if (countTokens(text.substring(previous, end)) > overlap) {
                break;
            }
            result = previous;
        }
        return result;
    }

    /** @param text 正文 @return cl100k_base实际Token数，保留特殊标记的普通文本语义 */
    public int countTokens(String text) {
        return ENCODING.countTokensOrdinary(text);
    }
}
