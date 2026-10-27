package org.etd.ai.knowledge.controller.vo;

import java.util.List;
import lombok.Data;

/** 批量删除响应；请求处理完成不代表全部成功，前端必须检查失败列表。 */
@Data
public class KnowledgeDocumentDeleteVO {
    /** 已成功删除的资料ID，前端据此清理已选项。 */
    private List<Long> successIds;

    /** 删除失败项，前端应保留选择并展示原因，以便用户重试。 */
    private List<Failure> failures;

    /** 批量删除页面的一项失败提示。 */
    @Data
    public static class Failure {
        /** 删除失败的资料ID。 */
        private Long id;
        /** 脱敏后的业务失败原因。 */
        private String message;
    }
}
