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
        SystemCommonCode systemCode = SystemCommonCode.create("CATEGORY", "A", "System Alpha", 1,
                CommonCodeKind.DYNAMIC, true, null, null, "tester", null);
        given(systemCommonCodeService.findActive("CATEGORY")).willReturn(List.of(systemCode));

        DwCommonCodeSnapshot dwCode = new DwCommonCodeSnapshot("CATEGORY", "A", "DW Alpha", 2,
                true, "DW", "desc", "{\"source\":\"dw\"}");
        given(dwCommonCodeDirectoryService.findActive("CATEGORY")).willReturn(List.of(dwCode));

        List<CommonCodeItem> result = service.aggregate("category", true, true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().codeName()).isEqualTo("System Alpha");
    }

    @Test
    @DisplayName("DW 코드만 포함하도록 요청하면 시스템 코드는 무시한다")
    void givenOnlyDwRequested_whenAggregate_thenUseDwOnly() {
        DwCommonCodeSnapshot dwCode = new DwCommonCodeSnapshot("CATEGORY", "B", "DW Beta", 1,
                true, "DW", "desc", "{}");
        given(dwCommonCodeDirectoryService.findActive("CATEGORY")).willReturn(List.of(dwCode));

        List<CommonCodeItem> result = service.aggregate("category", false, true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().codeValue()).isEqualTo("B");
        assertThat(result.getFirst().source()).isEqualTo(com.example.server.commoncode.model.CommonCodeSource.DW);
    }

    @Test
    @DisplayName("includeSystem/Dw 둘 다 false이면 빈 리스트를 반환한다")
    void givenBothSourcesDisabled_thenReturnEmpty() {
        List<CommonCodeItem> result = service.aggregate("category", false, false);
        assertThat(result).isEmpty();
    }
}
