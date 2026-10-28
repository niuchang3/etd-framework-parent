package org.etd.framework.starter.ai.document.docling.converter;

import ai.docling.serve.api.chunk.request.options.HierarchicalChunkerOptions;
import ai.docling.serve.api.chunk.request.options.HybridChunkerOptions;
import ai.docling.serve.api.chunk.response.Chunk;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.chunk.response.Document;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.framework.starter.ai.document.model.DocumentChunk;
import org.etd.framework.starter.ai.document.model.DocumentChunkDocument;
import org.etd.framework.starter.ai.document.model.DocumentChunkResult;
import org.etd.framework.starter.ai.document.model.DocumentParseResult;
import org.etd.framework.starter.ai.document.model.HierarchicalDocumentChunkOptions;
import org.etd.framework.starter.ai.document.model.HybridDocumentChunkOptions;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import ai.docling.core.DoclingDocument;
import ai.docling.serve.api.chunk.response.ExportDocumentResponse;

/** 原生分块选项及结果的 MapStruct 转换；业务不接触 Docling SDK 对象。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class DoclingChunkConverter {
    private final DoclingResultConverter resultConverter = Mappers.getMapper(DoclingResultConverter.class);
    private final DoclingStructureReader structureReader = new DoclingStructureReader();

    /** @param options 公共层级分块选项 @return SDK 选项 */
    public abstract HierarchicalChunkerOptions toSdk(HierarchicalDocumentChunkOptions options);

    /** @param options 公共混合分块选项 @return SDK 选项，空参数由服务决定，不伪造 tokenizer 默认值 */
    public abstract HybridChunkerOptions toSdk(HybridDocumentChunkOptions options);

    /** @param chunk SDK 分块结果 @return 公共片段及来源信息 */
    @Mapping(target = "index", source = "chunkIndex")
    @Mapping(target = "tokenCount", source = "numTokens")
    @Mapping(target = "documentItemRefs", source = "docItems")
    public abstract DocumentChunk toChunk(Chunk chunk);

    /** @param response 同步或异步取得的分块结果 @return 公共结果，空列表不代表完整成功 */
    @Mapping(target = "processingSeconds", source = "processingTime")
    public abstract DocumentChunkResult toResult(ChunkDocumentResponse response);

    /**
     * 提取附带原文结构，并保留独立的文档级状态。
     * @param document 引擎返回的文档结果
     * @return 原文结果，不把文档失败状态抛弃
     * @throws DocumentParseException 状态未知或成功文档缺少请求的 JSON
     */
    public DocumentChunkDocument toDocument(Document document) {
        if (document == null) {
            throw new DocumentParseException("分块结果包含空文档", DocumentParseException.NO_HTTP_STATUS, null);
        }
        DocumentParseStatus status = resultConverter.toParseStatus(document.getStatus());
        ExportDocumentResponse source = document.getContent();
        boolean hasContent = status == DocumentParseStatus.SUCCESS || status == DocumentParseStatus.PARTIAL_SUCCESS;
        if (hasContent && (source == null || source.getJsonContent() == null)) {
            throw new DocumentParseException("分块结果缺少请求的原文结构", DocumentParseException.NO_HTTP_STATUS, null);
        }
        DoclingDocument original = source == null ? null : source.getJsonContent();
        DoclingContent content = structureReader.read(original);
        DocumentParseResult result = resultConverter.toResult(document, content);
        String filename = source == null ? null : source.getFilename();
        return assembleDocument(filename, result);
    }

    /** @param filename 文件名 @param result 解析产物 @return 公共文档结果 */
    protected abstract DocumentChunkDocument assembleDocument(String filename, DocumentParseResult result);
}
