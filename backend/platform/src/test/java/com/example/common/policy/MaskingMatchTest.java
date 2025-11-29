package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.masking.DataKind;

@DisplayName("MaskingMatch")
class MaskingMatchTest {

    @Test
    @DisplayName("빌더로 모든 필드를 설정할 수 있다")
    void builderCoversFields() {
        UUID id = UUID.randomUUID();
        MaskingMatch match = MaskingMatch.builder()
                .policyId(id)
                .dataKinds(Set.of(DataKind.SSN))
                .maskingEnabled(true)
                .auditEnabled(true)
                .priority(10)
                .build();

        assertThat(match.getPolicyId()).isEqualTo(id);
        assertThat(match.getDataKinds()).containsExactly(DataKind.SSN);
        assertThat(match.isMaskingEnabled()).isTrue();
        assertThat(match.isAuditEnabled()).isTrue();
        assertThat(match.getPriority()).isEqualTo(10);
    }

    @Test
    @DisplayName("equals와 hashCode가 올바르게 동작한다")
    void equalsAndHashCode() {
        UUID id = UUID.randomUUID();
        MaskingMatch match1 = MaskingMatch.builder()
                .policyId(id)
                .dataKinds(Set.of(DataKind.PHONE))
                .maskingEnabled(true)
                .auditEnabled(false)
                .priority(1)
                .build();
        MaskingMatch match2 = MaskingMatch.builder()
                .policyId(id)
                .dataKinds(Set.of(DataKind.PHONE))
                .maskingEnabled(true)
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
                .dataKinds(Set.of(DataKind.EMAIL))
                .maskingEnabled(true)
                .auditEnabled(true)
                .priority(5)
                .build();

        String str = match.toString();
        assertThat(str).contains("MaskingMatch");
        assertThat(str).contains("EMAIL");
    }

    @Test
    @DisplayName("dataKinds가 빈 Set일 수 있다")
    void dataKindsCanBeEmpty() {
        MaskingMatch match = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .dataKinds(Set.of())
                .maskingEnabled(true)
                .auditEnabled(false)
                .priority(1)
                .build();

        assertThat(match.getDataKinds()).isEmpty();
    }

    @Test
    @DisplayName("maskingEnabled가 false일 수 있다 (화이트리스트)")
    void maskingEnabledCanBeFalse() {
        MaskingMatch match = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .dataKinds(Set.of(DataKind.SSN))
                .maskingEnabled(false)
                .auditEnabled(false)
                .priority(1)
                .build();

        assertThat(match.isMaskingEnabled()).isFalse();
    }

    @Nested
    @DisplayName("appliesTo 메서드")
    class AppliesTo {

        @Test
        @DisplayName("dataKinds가 null이면 모든 DataKind에 매칭된다")
        void matchesAllWhenNull() {
            MaskingMatch match = MaskingMatch.builder()
                    .policyId(UUID.randomUUID())
                    .dataKinds(null)
                    .maskingEnabled(true)
                    .priority(1)
                    .build();

            assertThat(match.appliesTo(DataKind.SSN)).isTrue();
            assertThat(match.appliesTo(DataKind.PHONE)).isTrue();
            assertThat(match.appliesTo(null)).isTrue();
        }

        @Test
        @DisplayName("dataKinds가 비어있으면 모든 DataKind에 매칭된다")
        void matchesAllWhenEmpty() {
            MaskingMatch match = MaskingMatch.builder()
                    .policyId(UUID.randomUUID())
                    .dataKinds(Set.of())
                    .maskingEnabled(true)
                    .priority(1)
                    .build();

            assertThat(match.appliesTo(DataKind.SSN)).isTrue();
            assertThat(match.appliesTo(DataKind.EMAIL)).isTrue();
        }

        @Test
        @DisplayName("dataKinds에 포함된 DataKind만 매칭된다")
        void matchesOnlyIncluded() {
            MaskingMatch match = MaskingMatch.builder()
                    .policyId(UUID.randomUUID())
                    .dataKinds(Set.of(DataKind.SSN, DataKind.PHONE))
                    .maskingEnabled(true)
                    .priority(1)
                    .build();

            assertThat(match.appliesTo(DataKind.SSN)).isTrue();
            assertThat(match.appliesTo(DataKind.PHONE)).isTrue();
            assertThat(match.appliesTo(DataKind.EMAIL)).isFalse();
            assertThat(match.appliesTo(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("레거시 호환 메서드")
    class LegacyCompatibility {

        @Test
        @DisplayName("getDataKind()는 첫 번째 dataKind를 반환한다")
        void getDataKindReturnsFirst() {
            MaskingMatch match = MaskingMatch.builder()
                    .policyId(UUID.randomUUID())
                    .dataKinds(Set.of(DataKind.SSN))
                    .maskingEnabled(true)
                    .priority(1)
                    .build();

            assertThat(match.getDataKind()).isEqualTo("SSN");
        }

        @Test
        @DisplayName("getDataKind()는 빈 Set일 때 null을 반환한다")
        void getDataKindReturnsNullWhenEmpty() {
            MaskingMatch match = MaskingMatch.builder()
                    .policyId(UUID.randomUUID())
                    .dataKinds(Set.of())
                    .maskingEnabled(true)
                    .priority(1)
                    .build();

            assertThat(match.getDataKind()).isNull();
        }

        @Test
        @DisplayName("dataKind(String) 빌더는 레거시 호환성을 제공한다")
        void legacyBuilderWorks() {
            MaskingMatch match = MaskingMatch.builder()
                    .policyId(UUID.randomUUID())
                    .dataKind("SSN")
                    .maskingEnabled(true)
                    .priority(1)
                    .build();

            assertThat(match.getDataKinds()).containsExactly(DataKind.SSN);
        }
    }
}
