package com.example.draft.application.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditRequestContextHolderTest {

    @Test
    @DisplayName("스레드로컬 컨텍스트를 설정/조회/초기화할 수 있다")
    void setGetAndClearContext() {
        AuditRequestContext context = new AuditRequestContext("127.0.0.1", "JUnit");

        AuditRequestContextHolder.set(context);
        assertThat(AuditRequestContextHolder.current()).contains(context);

        AuditRequestContextHolder.clear();
        assertThat(AuditRequestContextHolder.current()).isEmpty();
    }
}
