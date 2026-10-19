package org.etd.ai.model;

import jakarta.validation.Validation;
import org.etd.ai.model.controller.dto.ModelConnectionSaveDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.framework.starter.web.validation.UpdateRequest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 共用新增/更新DTO时，仅更新分组要求回传版本号，其他默认字段约束仍执行。 */
class UpdateVersionValidationTest {
    /** 新增可省略version，更新缺失version必须在接口校验阶段拒绝。 */
    @Test
    void shouldRequireVersionOnlyForUpdates() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Object dto : List.of(new ModelConnectionSaveDTO(),
                    new KnowledgeFolderSaveDTO())) {
                assertFalse(validator.validate(dto).stream()
                        .anyMatch(v -> "version".equals(v.getPropertyPath().toString())));
                assertTrue(validator.validate(dto, UpdateRequest.class).stream()
                        .anyMatch(v -> "version".equals(v.getPropertyPath().toString())));
                assertTrue(validator.validate(dto, UpdateRequest.class).stream()
                        .anyMatch(v -> !"version".equals(v.getPropertyPath().toString())));
            }
        }
    }
}
