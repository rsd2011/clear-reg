package com.example.server.commoncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.SystemCommonCode;
import com.example.server.commoncode.repository.SystemCommonCodeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemCommonCodeService 테스트")
class SystemCommonCodeServiceTest {

    @Mock
    private SystemCommonCodeRepository repository;

    private SystemCommonCodeService service;

    @BeforeEach
    void setUp() {
        service = new SystemCommonCodeService(repository);
    }

    @Test
    @DisplayName("Given 코드 목록 When findActive 호출 Then 비활성 코드를 제외한다")
    void givenExistingCodes_whenFindActive_thenFiltersInactive() {
        SystemCommonCode active = new SystemCommonCode();
        active.setCodeType("CATEGORY");
        active.setCodeValue("A");
        active.setCodeName("Alpha");
        active.setCodeKind(CommonCodeKind.DYNAMIC);
        active.setActive(true);

        SystemCommonCode inactive = active.copy();
        inactive.setActive(false);
        given(repository.findByCodeTypeOrderByDisplayOrderAscCodeValueAsc("CATEGORY"))
                .willReturn(List.of(active, inactive));

        List<SystemCommonCode> result = service.findActive("CATEGORY");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCodeValue()).isEqualTo("A");
    }

    @Test
    @DisplayName("Given 신규 코드 When create 호출 Then 타입을 정규화해 저장한다")
    void givenNewCode_whenCreate_thenPersistedWithNormalizedType() {
        SystemCommonCode request = new SystemCommonCode();
        request.setCodeValue("A");
        request.setCodeName("Alpha");
        request.setDisplayOrder(5);
        request.setCodeKind(CommonCodeKind.DYNAMIC);
        request.setActive(true);
        request.setUpdatedBy("tester");
        given(repository.findByCodeTypeAndCodeValue("CATEGORY", "A")).willReturn(Optional.empty());
        given(repository.save(any(SystemCommonCode.class))).willAnswer(invocation -> invocation.getArgument(0));

        SystemCommonCode result = service.create("category", request);

        assertThat(result.getCodeType()).isEqualTo("CATEGORY");
        ArgumentCaptor<SystemCommonCode> captor = ArgumentCaptor.forClass(SystemCommonCode.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCodeKind()).isEqualTo(CommonCodeKind.DYNAMIC);
    }

    @Test
    @DisplayName("Given 정적 코드 타입 When 동적 Kind 생성 시도 Then 예외를 던진다")
    void givenStaticType_whenNonStaticKindProvided_thenThrows() {
        SystemCommonCode request = new SystemCommonCode();
        request.setCodeValue("FILE");
        request.setCodeName("File");
        request.setCodeKind(CommonCodeKind.DYNAMIC);
        request.setActive(true);

        assertThatThrownBy(() -> service.create("file_classification", request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
