package org.etd.upms.dict.biz;

import org.etd.upms.dict.controller.vo.SystemDictDataVO;
import org.etd.upms.dict.controller.vo.SystemDictTypeVO;
import org.etd.upms.dict.service.SystemDictDataService;
import org.etd.upms.dict.service.SystemDictTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemDictBizServiceTest {

    @Mock
    private SystemDictTypeService dictTypeService;

    @Mock
    private SystemDictDataService dictDataService;

    private SystemDictBizService dictBizService;

    @BeforeEach
    void setUp() {
        dictBizService = new SystemDictBizService();
        ReflectionTestUtils.setField(dictBizService, "dictTypeService", dictTypeService);
        ReflectionTestUtils.setField(dictBizService, "dictDataService", dictDataService);
    }

    @Test
    void shouldQueryMultipleTypeCodesAndKeepRequestedOrder() {
        SystemDictTypeVO genderType = type(1L, "gender");
        SystemDictTypeVO statusType = type(2L, "status");
        SystemDictDataVO male = data(1L, "male");
        SystemDictDataVO enabled = data(2L, "enabled");
        when(dictTypeService.selectEnabledByCodes(anyCollection()))
                .thenReturn(List.of(statusType, genderType));
        when(dictDataService.selectEnabledByTypeIds(anyCollection()))
                .thenReturn(List.of(enabled, male));

        Map<String, List<SystemDictDataVO>> result = dictBizService
                .selectEnabledDataByTypeCodes(List.of("gender", "missing", "status", "gender"));

        assertThat(result.keySet()).containsExactly("gender", "missing", "status");
        assertThat(result.get("gender")).containsExactly(male);
        assertThat(result.get("missing")).isEmpty();
        assertThat(result.get("status")).containsExactly(enabled);
        verify(dictDataService).selectEnabledByTypeIds(List.of(2L, 1L));
    }

    private SystemDictTypeVO type(Long id, String typeCode) {
        SystemDictTypeVO type = new SystemDictTypeVO();
        type.setId(id);
        type.setTypeCode(typeCode);
        return type;
    }

    private SystemDictDataVO data(Long typeId, String dictCode) {
        SystemDictDataVO data = new SystemDictDataVO();
        data.setDictTypeId(typeId);
        data.setDictCode(dictCode);
        return data;
    }
}
