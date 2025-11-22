package com.example.server.commoncode.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemCommonCodeTest {

    @Test
    @DisplayName("codeType이 null이면 null로 유지하고, 값이 있으면 대문자로 저장한다")
    void setCodeType_handlesNullAndUppercase() {
        SystemCommonCode code = new SystemCommonCode();
        code.setCodeType(null);
        assertThat(code.getCodeType()).isNull();

        code.setCodeType("hr");
        assertThat(code.getCodeType()).isEqualTo("HR");
    }

    @Test
    @DisplayName("codeKind가 null이면 DYNAMIC으로 기본 설정한다")
    void of_defaultsCodeKindWhenNull() {
        SystemCommonCode draft = new SystemCommonCode();
        draft.setCodeValue("V");
        draft.setCodeName("Name");
        draft.setDisplayOrder(1);
        draft.setCodeKind(null);
        draft.setActive(true);
        draft.setDescription("desc");
        draft.setMetadataJson("{}");
        draft.setUpdatedBy("actor");

        SystemCommonCode copied = SystemCommonCode.of("TYPE", draft);

        assertThat(copied.getCodeType()).isEqualTo("TYPE");
        assertThat(copied.getCodeKind()).isEqualTo(CommonCodeKind.DYNAMIC);
    }

    @Test
    @DisplayName("copy는 모든 필드를 그대로 복사한다")
    void copyCopiesFields() {
        SystemCommonCode original = new SystemCommonCode();
        original.setCodeType("TYPE");
        original.setCodeValue("V");
        original.setCodeName("Name");
        original.setDisplayOrder(2);
        original.setCodeKind(CommonCodeKind.STATIC);
        original.setActive(false);
        original.setDescription("desc");
        original.setMetadataJson("{\"a\":1}");
        original.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        original.setUpdatedBy("actor");

        SystemCommonCode copy = original.copy();

        assertThat(copy.getCodeType()).isEqualTo(original.getCodeType());
        assertThat(copy.getCodeValue()).isEqualTo(original.getCodeValue());
        assertThat(copy.getCodeKind()).isEqualTo(CommonCodeKind.STATIC);
        assertThat(copy.isActive()).isFalse();
        assertThat(copy.getMetadataJson()).contains("a");
    }
}
