package org.etd.ai.knowledge;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.etd.ai.knowledge.controller.dto.KnowledgeChunkSaveDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeChunkSplitDTO;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;
import jakarta.validation.Validation;
import org.etd.ai.knowledge.controller.dto.KnowledgeChunkGenerateDTO;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import static org.assertj.core.api.Assertions.*;

/** 编辑正文和规则允许请求读取，但不进入框架异常cURL日志的JSON序列化。 */
class KnowledgeChunkRequestTest {
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    /** WRITE_ONLY不影响正文写入，日志JSON不包含上传内容。 */
    @Test
    void acceptsContentButHidesItFromSerialization() throws Exception {
        String body = "{\"version\":2,\"chunks\":[{\"id\":1,\"content\":\"敏感正文\"}]}";
        KnowledgeChunkSaveDTO request = json.readValue(body, KnowledgeChunkSaveDTO.class);
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
        KnowledgeChunkGenerateDTO request = json.readValue(body, KnowledgeChunkGenerateDTO.class);
        JsonNode payload = json.valueToTree(request.getParameters());
        assertThat(payload.path("advanced").path("documentTimeout").asText()).isEqualTo("PT1M30S");
        assertThat(json.writeValueAsString(request)).doesNotContain("parameters");
    }
    /** 拆分正文只用于请求，不能进入参数日志或异常cURL，两个正文独立校验。 */
    @Test
    void validatesSplitContentsAndHidesThemFromLogs() throws Exception {
        String body = """
                {"version":2,"firstContent":"拆分前半段","secondContent":"拆分后半段"}
                """;
        KnowledgeChunkSplitDTO input = json.readValue(body, KnowledgeChunkSplitDTO.class);
        assertThat(input.getFirstContent()).isEqualTo("拆分前半段");
        assertThat(input.getSecondContent()).isEqualTo("拆分后半段");
        assertThat(json.writeValueAsString(input)).doesNotContain("拆分前半段", "拆分后半段");
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(input)).isEmpty();
            input.setVersion(-1L);
            input.setFirstContent(" ");
            input.setSecondContent("字".repeat(KnowledgeChunkLimits.MAX_CONTENT_CHARACTERS + 1));
            assertThat(validator.validate(input)).extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("version", "firstContent", "secondContent");
        }
    }

    /** 同一个分片ID出现两次时明确拒绝，不用后提交内容静默覆盖前者。 */
    @Test
    void rejectsDuplicateEditIds() throws Exception {
        String body = "{\"version\":2,\"chunks\":[{\"id\":1,\"content\":\"A\"},{\"id\":1,\"content\":\"B\"}]}";
        KnowledgeChunkSaveDTO request = json.readValue(body, KnowledgeChunkSaveDTO.class);
        assertThatThrownBy(() -> KnowledgeChunkConverter.INSTANCE.toEditValues(request.getChunks()))
                .isInstanceOf(ApiRuntimeException.class);
    }
}
