package com.example.admin.codemanage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemCommonCodeType fromCode 분기")
class SystemCommonCodeTypeTest {

    @Test
    @DisplayName("코드가 null이면 empty를 반환한다")
    void returnsEmptyWhenCodeNull() {
        assertThat(SystemCommonCodeType.fromCode(null)).isEmpty();
    }

    @Test
    @DisplayName("코드가 일치하면 타입을 반환한다")
    void returnsTypeWhenMatched() {
        Optional<SystemCommonCodeType> type = SystemCommonCodeType.fromCode("notice_category");
        assertThat(type).contains(SystemCommonCodeType.NOTICE_CATEGORY);
        assertThat(type.get().defaultKind()).isEqualTo(CodeManageKind.DYNAMIC);
    }

    @Test
    @DisplayName("코드가 불일치하면 empty")
    void returnsEmptyWhenNoMatch() {
        assertThat(SystemCommonCodeType.fromCode("unknown")).isEmpty();
    }
}
