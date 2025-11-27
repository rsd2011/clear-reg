package com.example.admin.codemanage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.codemanage.model.CommonCodeKind;
import com.example.admin.codemanage.model.SystemCommonCode;
import com.example.admin.codemanage.repository.SystemCommonCodeRepository;

class SystemCommonCodeServiceTest {

    private final SystemCommonCodeRepository repository = mock(SystemCommonCodeRepository.class);
    private final SystemCommonCodeService service = new SystemCommonCodeService(repository);

    @Test
    @DisplayName("codeType이 null이면 예외가 발생한다")
    void normalizeTypeNullThrows() {
        assertThatThrownBy(() -> service.findAll(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("STATIC 유형에 다른 kind를 주면 예외가 발생한다")
    void enforceKindStaticOnly() {
        SystemCommonCode request = SystemCommonCode.create("FILE_CLASSIFICATION", "CLS", "name", 0,
                CommonCodeKind.DYNAMIC, true, null, null, null, null); // STATIC이어야 하는 유형에 DYNAMIC 전달
        given(repository.findByCodeTypeAndCodeValue(eq("FILE_CLASSIFICATION"), any())).willReturn(Optional.empty());
        given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.create("FILE_CLASSIFICATION", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STATIC");
    }

    @Test
    @DisplayName("이미 존재하는 코드이면 생성 시 예외를 던진다")
    void createThrowsWhenDuplicateExists() {
        SystemCommonCode existing = SystemCommonCode.create("LANG", "KO", "Korean", 0,
                CommonCodeKind.DYNAMIC, true, null, null, "sys", null);
        given(repository.findByCodeTypeAndCodeValue(eq("LANG"), any()))
                .willReturn(Optional.of(existing));

        SystemCommonCode request = SystemCommonCode.create("lang", "KO", "Korean", 0,
                CommonCodeKind.DYNAMIC, true, null, null, "tester", null);

        assertThatThrownBy(() -> service.create("lang", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재");
    }

    @Test
    @DisplayName("update는 존재하지 않으면 예외를 던진다")
    void updateThrowsWhenNotFound() {
        given(repository.findByCodeTypeAndCodeValue(eq("LANG"), eq("EN")))
                .willReturn(Optional.empty());

        SystemCommonCode request = SystemCommonCode.create("LANG", "EN", "English", 0,
                CommonCodeKind.DYNAMIC, true, null, null, null, null);

        assertThatThrownBy(() -> service.update("LANG", "EN", request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update는 null updatedBy를 system으로 대체한다")
    void updateSetsDefaultUpdatedBy() {
        SystemCommonCode existing = SystemCommonCode.create("LANG", "EN", "English", 0,
                CommonCodeKind.STATIC, true, null, null, "old", null);
        given(repository.findByCodeTypeAndCodeValue(eq("LANG"), eq("EN")))
                .willReturn(Optional.of(existing));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        SystemCommonCode request = SystemCommonCode.create("LANG", "EN", "English", 0,
                CommonCodeKind.STATIC, true, null, null, null, null);

        SystemCommonCode result = service.update("LANG", "EN", request);

        assertThat(result.getUpdatedBy()).isEqualTo("system");
        assertThat(result.getCodeKind()).isEqualTo(CommonCodeKind.STATIC);
    }

    @Test
    @DisplayName("findActive는 리포지토리 결과를 copy하여 반환한다")
    void findActiveCopies() {
        SystemCommonCode entity = SystemCommonCode.create("LANG", "ko", "Korean", 0,
                CommonCodeKind.DYNAMIC, true, null, null, "tester", null);
        given(repository.findByCodeTypeOrderByDisplayOrderAscCodeValueAsc("LANG"))
                .willReturn(java.util.List.of(entity));

        assertThat(service.findActive("LANG")).singleElement()
                .extracting(SystemCommonCode::getCodeValue)
                .isEqualTo("ko");
        verify(repository).findByCodeTypeOrderByDisplayOrderAscCodeValueAsc("LANG");
    }
}
