package com.example.server.commoncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.server.commoncode.model.SystemCommonCode;

@ExtendWith(MockitoExtension.class)
class SystemCommonCodeServiceLineCoverTest {

    @Mock
    com.example.server.commoncode.repository.SystemCommonCodeRepository repository;

    @InjectMocks
    SystemCommonCodeService service;

    @Test
    @DisplayName("create: 중복 코드가 있으면 IllegalArgumentException을 던진다")
    void createThrowsOnDuplicate() {
        SystemCommonCode req = SystemCommonCode.create("TYPE", "VAL", "NAME", 0,
                null, true, null, null, null, null);
        given(repository.findByCodeTypeAndCodeValue("TYPE", "VAL")).willReturn(Optional.of(req.copy()));

        assertThatThrownBy(() -> service.create("TYPE", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("normalizeType: codeType이 null이면 IllegalArgumentException")
    void normalizeTypeNull() {
        SystemCommonCode req = SystemCommonCode.create(null, "VAL", "NAME", 0,
                null, true, null, null, null, null);

        assertThatThrownBy(() -> service.findActive(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create: STATIC 기본 타입에서 DYNAMIC을 요청하면 예외를 던진다")
    void enforceKindWhenStaticType() {
        SystemCommonCode req = SystemCommonCode.create("FILE_CLASSIFICATION", "001", "파일 분류", 0,
                com.example.server.commoncode.model.CommonCodeKind.DYNAMIC, true, null, null, null, null);

        assertThatThrownBy(() -> service.create("FILE_CLASSIFICATION", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create: codeKind와 updatedBy가 null이면 기본값으로 채워 저장한다")
    void createFillsDefaultKindAndUpdatedBy() {
        SystemCommonCode req = SystemCommonCode.create("NOTICE_CATEGORY", "CAT", "카테고리", 0,
                null, true, null, null, null, null);

        given(repository.findByCodeTypeAndCodeValue("NOTICE_CATEGORY", "CAT")).willReturn(Optional.empty());

        // save에서 넘어온 엔티티의 필드 값을 검증하기 위해 캡처한다
        org.mockito.ArgumentCaptor<SystemCommonCode> captor = org.mockito.ArgumentCaptor.forClass(SystemCommonCode.class);
        given(repository.save(any(SystemCommonCode.class))).willAnswer(invocation -> invocation.getArgument(0, SystemCommonCode.class));

        var result = service.create("NOTICE_CATEGORY", req);

        verify(repository).save(captor.capture());
        SystemCommonCode savedArg = captor.getValue();

        assertThat(savedArg.getCodeKind()).isEqualTo(com.example.server.commoncode.model.CommonCodeKind.DYNAMIC);
        assertThat(savedArg.getUpdatedBy()).isEqualTo("system");
        assertThat(result.getCodeKind()).isEqualTo(com.example.server.commoncode.model.CommonCodeKind.DYNAMIC);
    }

    @Test
    @DisplayName("update: 엔티티가 없으면 IllegalArgumentException")
    void updateThrowsWhenMissing() {
        given(repository.findByCodeTypeAndCodeValue("NOTICE_CATEGORY", "MISSING")).willReturn(Optional.empty());

        SystemCommonCode req = SystemCommonCode.create("NOTICE_CATEGORY", "MISSING", "name", 0,
                null, true, null, null, null, null);

        assertThatThrownBy(() -> service.update("NOTICE_CATEGORY", "MISSING", req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
