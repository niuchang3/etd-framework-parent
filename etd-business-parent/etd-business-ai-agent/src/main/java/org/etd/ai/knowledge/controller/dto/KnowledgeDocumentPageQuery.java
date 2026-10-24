package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 知识资料分页条件，批量目录由前端收集当前目录及其子孙目录后传入。 */
@Data
public class KnowledgeDocumentPageQuery {

    /** 页码，从 1 开始，默认查询第一页。 */
    @Min(1)
    private long current = 1;

    /** 每页条数，默认 10 条，最多 200 条。 */
    @Min(1)
    @Max(200)
    private long size = 10;

    /** 单目录筛选，仅在 folderIds 为空时生效。 */
    @Positive
    private Long folderId;

    /** 优先使用的目录 ID 集合，包含当前目录及需查询的子孙目录，元素必须为正整数。 */
    private List<@NotNull @Positive Long> folderIds;

    /** 资料名称关键字，最多 255 个字符。 */
    @Size(max = 255)
    private String keyword;

    /** 上传状态，未传时不按上传状态筛选。 */
    private String uploadStatus;

    /** 分片状态：unprocessed、generating、draft、confirmed、failed；未传或空白时不筛选。 */
    private String chunkStatus;

}
