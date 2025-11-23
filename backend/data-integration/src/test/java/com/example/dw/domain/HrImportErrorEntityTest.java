package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HrImportErrorEntityTest {

    @Test
    @DisplayName("ImportError 엔티티는 필드 값을 보존한다")
    void storesFields() {
        HrImportErrorEntity entity = new HrImportErrorEntity();
        entity.setLineNumber(10);
        entity.setErrorMessage("bad data");
        assertThat(entity.getLineNumber()).isEqualTo(10);
        assertThat(entity.getErrorMessage()).isEqualTo("bad data");
    }
}
