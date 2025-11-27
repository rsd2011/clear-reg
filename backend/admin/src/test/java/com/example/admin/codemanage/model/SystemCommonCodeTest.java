package com.example.admin.codemanage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemCommonCodeTest {

    @Test
    @DisplayName("codeType이 null이면 null로 유지하고, 값이 있으면 대문자로 저장한다")
    void setCodeType_handlesNullAndUppercase() {
        SystemCommonCode code = SystemCommonCode.create(null, "V", "Name", 0,
                null, true, null, null, "actor", null);
        assertThat(code.getCodeType()).isNull();

        SystemCommonCode upper = SystemCommonCode.create("hr", "V", "Name", 0,
                null, true, null, null, "actor", null);
        assertThat(upper.getCodeType()).isEqualTo("HR");
    }

    @Test
    @DisplayName("codeKind가 null이면 DYNAMIC으로 기본 설정한다")
    void of_defaultsCodeKindWhenNull() {
        SystemCommonCode draft = SystemCommonCode.create("TYPE", "V", "Name", 1,
                null, true, "desc", "{}", "actor", null);

        SystemCommonCode copied = draft.copy();

        assertThat(copied.getCodeType()).isEqualTo("TYPE");
        assertThat(copied.getCodeKind()).isEqualTo(CodeManageKind.DYNAMIC);
    }

    @Test
    @DisplayName("copy는 모든 필드를 그대로 복사한다")
    void copyCopiesFields() {
        SystemCommonCode original = SystemCommonCode.create("TYPE", "V", "Name", 2,
                CodeManageKind.STATIC, false, "desc", "{\"a\":1}", "actor", OffsetDateTime.now(ZoneOffset.UTC));

        SystemCommonCode copy = original.copy();

        assertThat(copy.getCodeType()).isEqualTo(original.getCodeType());
        assertThat(copy.getCodeValue()).isEqualTo(original.getCodeValue());
        assertThat(copy.getCodeKind()).isEqualTo(CodeManageKind.STATIC);
        assertThat(copy.isActive()).isFalse();
        assertThat(copy.getMetadataJson()).contains("a");
    }
}
