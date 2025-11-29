package com.example.admin.maskingpolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;

@DisplayName("MaskingPolicy 엔티티")
class MaskingPolicyTest {

    @Nested
    @DisplayName("isEffectiveAt 메서드는")
    class IsEffectiveAt {

        @Test
        @DisplayName("active가 false면 false를 반환한다")
        void returnsFalseWhenInactive() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .active(false)
                    .build();

            assertThat(policy.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("effectiveFrom 이전이면 false를 반환한다")
        void returnsFalseBeforeEffectiveFrom() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .active(true)
                    .effectiveFrom(now.plusSeconds(100))
                    .build();

            assertThat(policy.isEffectiveAt(now)).isFalse();
        }

        @Test
        @DisplayName("effectiveTo 이후면 false를 반환한다")
        void returnsFalseAfterEffectiveTo() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .active(true)
                    .effectiveTo(now.minusSeconds(100))
                    .build();

            assertThat(policy.isEffectiveAt(now)).isFalse();
        }

        @Test
        @DisplayName("유효 기간 내면 true를 반환한다")
        void returnsTrueWhenWithinRange() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .active(true)
                    .effectiveFrom(now.minusSeconds(100))
                    .effectiveTo(now.plusSeconds(100))
                    .build();

            assertThat(policy.isEffectiveAt(now)).isTrue();
        }

        @Test
        @DisplayName("effectiveFrom/To가 null이면 항상 유효하다")
        void returnsTrueWhenNoDateBounds() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.isEffectiveAt(Instant.now())).isTrue();
        }
    }

    @Nested
    @DisplayName("matches 메서드는")
    class Matches {

        @Test
        @DisplayName("featureCode가 다르면 false를 반환한다")
        void returnsFalseWhenFeatureMismatch() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.CUSTOMER, null, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasActionButParamNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoActionButParamHas() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(null)
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasPermGroupButParamNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .permGroupCode("ADMIN")
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoPermGroupButParamHas() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .permGroupCode(null)
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, "ADMIN", null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 dataKind가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasDataKindButParamNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("SSN")
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 dataKind가 null이면 모든 종류에 매칭된다")
        void matchesAllWhenPolicyDataKindNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind(null)
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, "SSN", Instant.now())).isTrue();
            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, "PHONE", Instant.now())).isTrue();
            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, null, Instant.now())).isTrue();
        }

        @Test
        @DisplayName("dataKind가 대소문자 무관하게 매칭된다")
        void dataKindMatchesCaseInsensitive() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("SSN")
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, "ssn", Instant.now())).isTrue();
            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, "Ssn", Instant.now())).isTrue();
        }

        @Test
        @DisplayName("모든 조건이 매칭되면 true를 반환한다")
        void returnsTrueWhenAllMatch() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .permGroupCode("ADMIN")
                    .dataKind("SSN")
                    .maskRule("PARTIAL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, "ADMIN", "SSN", Instant.now())).isTrue();
        }

        @Test
        @DisplayName("actionCode가 다르면 false를 반환한다")
        void returnsFalseWhenActionMismatch() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.UPDATE, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("dataKind가 다르면 false를 반환한다")
        void returnsFalseWhenDataKindMismatch() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("SSN")
                    .maskRule("FULL")
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, "PHONE", Instant.now())).isFalse();
        }
    }

    @Test
    @DisplayName("빌더 기본값이 올바르게 설정된다")
    void builderDefaultValues() {
        MaskingPolicy policy = MaskingPolicy.builder()
                .featureCode(FeatureCode.ORGANIZATION)
                .maskRule("FULL")
                .build();

        assertThat(policy.getPriority()).isEqualTo(100);
        assertThat(policy.getActive()).isTrue();
        assertThat(policy.getAuditEnabled()).isFalse();
    }
}
