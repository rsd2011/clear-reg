package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingMatch")
class MaskingMatchTest {

    @Test
    @DisplayName("빌더로 모든 필드를 설정할 수 있다")
    void builderCoversFields() {
        UUID id = UUID.randomUUID();
        MaskingMatch match = MaskingMatch.builder()
                .policyId(id)
                .dataKind("SSN")
                .maskRule("PARTIAL")
                .maskParams("{\"visibleChars\":4}")
                .auditEnabled(true)
                .priority(10)
                .build();

        assertThat(match.getPolicyId()).isEqualTo(id);
        assertThat(match.getDataKind()).isEqualTo("SSN");
        assertThat(match.getMaskRule()).isEqualTo("PARTIAL");
        assertThat(match.getMaskParams()).isEqualTo("{\"visibleChars\":4}");
        assertThat(match.isAuditEnabled()).isTrue();
        assertThat(match.getPriority()).isEqualTo(10);
    }

    @Test
    @DisplayName("equals와 hashCode가 올바르게 동작한다")
    void equalsAndHashCode() {
        UUID id = UUID.randomUUID();
        MaskingMatch match1 = MaskingMatch.builder()
                .policyId(id)
                .dataKind("PHONE")
                .maskRule("FULL")
                .auditEnabled(false)
                .priority(1)
                .build();
        MaskingMatch match2 = MaskingMatch.builder()
                .policyId(id)
                .dataKind("PHONE")
                .maskRule("FULL")
                .auditEnabled(false)
                .priority(1)
                .build();

        assertThat(match1).isEqualTo(match2);
        assertThat(match1.hashCode()).isEqualTo(match2.hashCode());
    }

    @Test
    @DisplayName("toString이 올바르게 동작한다")
    void toStringWorks() {
        MaskingMatch match = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .dataKind("EMAIL")
                .maskRule("HASH")
                .auditEnabled(true)
                .priority(5)
                .build();

        String str = match.toString();
        assertThat(str).contains("MaskingMatch");
        assertThat(str).contains("EMAIL");
        assertThat(str).contains("HASH");
    }

    @Test
    @DisplayName("dataKind가 null일 수 있다")
    void dataKindCanBeNull() {
        MaskingMatch match = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .dataKind(null)
                .maskRule("FULL")
                .auditEnabled(false)
                .priority(1)
                .build();

        assertThat(match.getDataKind()).isNull();
    }

    @Test
    @DisplayName("maskParams가 null일 수 있다")
    void maskParamsCanBeNull() {
        MaskingMatch match = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .maskRule("NONE")
                .maskParams(null)
                .auditEnabled(false)
                .priority(1)
                .build();

        assertThat(match.getMaskParams()).isNull();
    }
}
