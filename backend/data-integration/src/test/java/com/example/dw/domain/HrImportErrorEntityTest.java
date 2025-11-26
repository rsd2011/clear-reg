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
        HrImportBatchEntity batch = HrImportBatchEntity.receive("f.csv", null, "SRC",
                java.time.LocalDate.now(), 1, "chk", "/tmp");
        HrImportErrorEntity entity = HrImportErrorEntity.of(batch, 10, "EMPLOYEE", "REF", "E100", "bad data", "raw");
        assertThat(entity.getLineNumber()).isEqualTo(10);
        assertThat(entity.getErrorMessage()).isEqualTo("bad data");
    }
}
