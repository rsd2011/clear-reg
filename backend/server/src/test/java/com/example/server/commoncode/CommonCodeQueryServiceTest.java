package com.example.server.commoncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.application.DwCommonCodeDirectoryService;
import com.example.dw.application.DwCommonCodeSnapshot;
import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.SystemCommonCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommonCodeQueryService 테스트")
class CommonCodeQueryServiceTest {

    @Mock
    private DwCommonCodeDirectoryService dwCommonCodeDirectoryService;
    @Mock
    private SystemCommonCodeService systemCommonCodeService;

    private CommonCodeQueryService service;

    @BeforeEach
    void setUp() {
        service = new CommonCodeQueryService(dwCommonCodeDirectoryService, systemCommonCodeService);
    }

    @Test
    @DisplayName("Given 시스템/ DW 코드 When aggregate 호출 Then 병합 후 중복을 제거한다")
    void givenSystemAndDwCodes_whenAggregate_thenMergeAndDeduplicate() {
        SystemCommonCode systemCode = new SystemCommonCode();
        systemCode.setCodeType("CATEGORY");
        systemCode.setCodeValue("A");
        systemCode.setCodeName("System Alpha");
        systemCode.setDisplayOrder(1);
        systemCode.setCodeKind(CommonCodeKind.DYNAMIC);
        systemCode.setActive(true);
        given(systemCommonCodeService.findActive("CATEGORY")).willReturn(List.of(systemCode));

        DwCommonCodeSnapshot dwCode = new DwCommonCodeSnapshot("CATEGORY", "A", "DW Alpha", 2,
                true, "DW", "desc", "{\"source\":\"dw\"}");
        given(dwCommonCodeDirectoryService.findActive("CATEGORY")).willReturn(List.of(dwCode));

        List<CommonCodeItem> result = service.aggregate("category", true, true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().codeName()).isEqualTo("System Alpha");
    }
}
