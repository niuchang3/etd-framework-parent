package org.etd.ai.knowledge;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.etd.ai.knowledge.controller.dto.KnowledgeChunkSaveDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeChunkGenerateDTO;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** 编辑正文和规则允许请求读取，但不进入框架异常cURL日志的JSON序列化。 */
class KnowledgeChunkRequestTest {
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    /** WRITE_ONLY不影响正文写入，日志JSON不包含上传内容。 */
    @Test
    void acceptsContentButHidesItFromSerialization() throws Exception {
        String body = "{\"version\":2,\"chunks\":[{\"id\":1,\"content\":\"敏感正文\"}]}";
        var request = json.readValue(body, KnowledgeChunkSaveDTO.class);
        assertThat(request.getChunks().getFirst().getContent()).isEqualTo("敏感正文");
        String serialized = json.writeValueAsString(request);
        assertThat(serialized).doesNotContain("敏感正文");
    }
    /** 三级参数可反序列化，处理超时稳定输出ISO-8601，不把规则写入日志。 */
    @Test
    void acceptsParametersAndKeepsDurationContract() throws Exception {
        String body = """
                {"version":0,"parameters":{"basic":{"strategy":"hybrid","maxTokens":128},
                "normal":{},"advanced":{"documentTimeout":"PT90S"}}}
                """;
        var request = json.readValue(body, KnowledgeChunkGenerateDTO.class);
        var payload = json.valueToTree(request.getParameters());
        assertThat(payload.path("advanced").path("documentTimeout").asText()).isEqualTo("PT1M30S");
        assertThat(json.writeValueAsString(request)).doesNotContain("parameters");
    }
    /** 同一个分片ID出现两次时明确拒绝，不用后提交内容静默覆盖前者。 */
    @Test
    void rejectsDuplicateEditIds() throws Exception {
        String body = "{\"version\":2,\"chunks\":[{\"id\":1,\"content\":\"A\"},{\"id\":1,\"content\":\"B\"}]}";
        var request = json.readValue(body, KnowledgeChunkSaveDTO.class);
        assertThatThrownBy(() -> KnowledgeChunkConverter.INSTANCE.toEditValues(request.getChunks()))
                .isInstanceOf(ApiRuntimeException.class);
    }
}
