package com.example.common.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValueObjectsBroadCoverageTest {

    @Test
    @DisplayName("PermissionGroupCode/BatchJobId/PaymentReference 등 기타 값 객체 스모크")
    void miscValueObjects() {
        assertThat(PermissionGroupCode.of("PG_VIEW").value()).isEqualTo("PG_VIEW");
        assertThat(BatchJobId.of("JOB-20250101-0001").toString()).contains("JOB");
        assertThat(PaymentReference.of("급여 메모 2025").masked()).contains("*");
    }
}
