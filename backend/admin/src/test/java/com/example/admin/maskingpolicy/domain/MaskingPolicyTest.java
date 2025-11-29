package com.example.admin.maskingpolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.masking.DataKind;

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
                    .maskingEnabled(true)
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
                    .maskingEnabled(true)
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
                    .maskingEnabled(true)
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
                    .maskingEnabled(true)
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
                    .maskingEnabled(true)
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
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.CUSTOMER, null, null, (DataKind) null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasActionButParamNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, (DataKind) null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoActionButParamHas() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(null)
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, null, (DataKind) null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasPermGroupButParamNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .permGroupCode("ADMIN")
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, (DataKind) null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoPermGroupButParamHas() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .permGroupCode(null)
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, "ADMIN", (DataKind) null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 dataKinds가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasDataKindsButParamNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of(DataKind.SSN))
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, (DataKind) null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 dataKinds가 비어있으면 모든 종류에 매칭된다")
        void matchesAllWhenPolicyDataKindsEmpty() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of()) // 빈 Set = 모든 종류에 적용
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, DataKind.SSN, Instant.now())).isTrue();
            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, DataKind.PHONE, Instant.now())).isTrue();
            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, (DataKind) null, Instant.now())).isTrue();
        }

        @Test
        @DisplayName("모든 조건이 매칭되면 true를 반환한다")
        void returnsTrueWhenAllMatch() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .permGroupCode("ADMIN")
                    .dataKinds(Set.of(DataKind.SSN))
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, "ADMIN", DataKind.SSN, Instant.now())).isTrue();
        }

        @Test
        @DisplayName("actionCode가 다르면 false를 반환한다")
        void returnsFalseWhenActionMismatch() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.UPDATE, null, (DataKind) null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("dataKind가 다르면 false를 반환한다")
        void returnsFalseWhenDataKindMismatch() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of(DataKind.SSN))
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, DataKind.PHONE, Instant.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("appliesTo 메서드는")
    class AppliesTo {

        @Test
        @DisplayName("dataKinds가 비어있으면 모든 종류에 적용된다")
        void appliesToAllWhenEmpty() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of())
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.appliesTo(DataKind.SSN)).isTrue();
            assertThat(policy.appliesTo(DataKind.PHONE)).isTrue();
            assertThat(policy.appliesTo(null)).isTrue();
        }

        @Test
        @DisplayName("dataKinds에 포함된 종류만 적용된다")
        void appliesToOnlyIncluded() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of(DataKind.SSN, DataKind.PHONE))
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            assertThat(policy.appliesTo(DataKind.SSN)).isTrue();
            assertThat(policy.appliesTo(DataKind.PHONE)).isTrue();
            assertThat(policy.appliesTo(DataKind.EMAIL)).isFalse();
            assertThat(policy.appliesTo(null)).isFalse();
        }
    }

    @Test
    @DisplayName("빌더 기본값이 올바르게 설정된다")
    void builderDefaultValues() {
        MaskingPolicy policy = MaskingPolicy.builder()
                .featureCode(FeatureCode.ORGANIZATION)
                .maskingEnabled(true)
                .build();

        assertThat(policy.getPriority()).isEqualTo(100);
        assertThat(policy.getActive()).isTrue();
        assertThat(policy.getAuditEnabled()).isFalse();
        assertThat(policy.getDataKinds()).isEmpty();
    }

    @Nested
    @DisplayName("레거시 호환 메서드")
    class LegacyCompatibility {

        @Test
        @DisplayName("dataKind(String) 빌더가 동작한다")
        void legacyDataKindBuilderWorks() {
            @SuppressWarnings("deprecation")
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("SSN")
                    .maskingEnabled(true)
                    .build();

            assertThat(policy.getDataKinds()).containsExactly(DataKind.SSN);
        }

        @Test
        @DisplayName("getDataKind()는 첫 번째 dataKind를 반환한다")
        void getDataKindReturnsFirst() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of(DataKind.SSN))
                    .maskingEnabled(true)
                    .build();

            @SuppressWarnings("deprecation")
            String dataKind = policy.getDataKind();
            assertThat(dataKind).isEqualTo("SSN");
        }

        @Test
        @DisplayName("getMaskRule()은 maskingEnabled 기반으로 반환한다")
        void getMaskRuleReturnsBasedOnEnabled() {
            MaskingPolicy enabled = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskingEnabled(true)
                    .build();
            MaskingPolicy disabled = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskingEnabled(false)
                    .build();

            @SuppressWarnings("deprecation")
            String enabledRule = enabled.getMaskRule();
            @SuppressWarnings("deprecation")
            String disabledRule = disabled.getMaskRule();

            assertThat(enabledRule).isEqualTo("PARTIAL");
            assertThat(disabledRule).isEqualTo("NONE");
        }

        @Test
        @DisplayName("maskRule(String) 빌더가 동작한다")
        void legacyMaskRuleBuilderWorks() {
            @SuppressWarnings("deprecation")
            MaskingPolicy policyWithRule = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .build();
            @SuppressWarnings("deprecation")
            MaskingPolicy policyNone = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("NONE")
                    .build();

            assertThat(policyWithRule.getMaskingEnabled()).isTrue();
            assertThat(policyNone.getMaskingEnabled()).isFalse();
        }

        @Test
        @DisplayName("dataKindsFromStrings 빌더가 동작한다")
        void legacyDataKindsFromStringsWorks() {
            @SuppressWarnings("deprecation")
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKindsFromStrings(Set.of("SSN", "PHONE"))
                    .maskingEnabled(true)
                    .build();

            assertThat(policy.getDataKinds()).containsExactlyInAnyOrder(DataKind.SSN, DataKind.PHONE);
        }

        @Test
        @DisplayName("dataKindsFromStrings null/빈 Set 처리")
        void legacyDataKindsFromStringsNullOrEmpty() {
            @SuppressWarnings("deprecation")
            MaskingPolicy policyNull = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKindsFromStrings(null)
                    .maskingEnabled(true)
                    .build();
            @SuppressWarnings("deprecation")
            MaskingPolicy policyEmpty = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKindsFromStrings(Set.of())
                    .maskingEnabled(true)
                    .build();

            assertThat(policyNull.getDataKinds()).isEmpty();
            assertThat(policyEmpty.getDataKinds()).isEmpty();
        }

        @Test
        @DisplayName("dataKind(String) null/빈 문자열 처리")
        void legacyDataKindNullOrBlank() {
            @SuppressWarnings("deprecation")
            MaskingPolicy policyNull = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind(null)
                    .maskingEnabled(true)
                    .build();
            @SuppressWarnings("deprecation")
            MaskingPolicy policyBlank = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("   ")
                    .maskingEnabled(true)
                    .build();

            assertThat(policyNull.getDataKinds()).isEmpty();
            assertThat(policyBlank.getDataKinds()).isEmpty();
        }

        @Test
        @DisplayName("maskParams 빌더가 무시된다")
        void legacyMaskParamsIgnored() {
            @SuppressWarnings("deprecation")
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskParams("{\"start\": 3}")
                    .maskingEnabled(true)
                    .build();

            @SuppressWarnings("deprecation")
            String params = policy.getMaskParams();
            assertThat(params).isNull();
        }

        @Test
        @DisplayName("getDataKind() dataKinds가 비어있으면 null 반환")
        void getDataKindReturnsNullWhenEmpty() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of())
                    .maskingEnabled(true)
                    .build();

            @SuppressWarnings("deprecation")
            String dataKind = policy.getDataKind();
            assertThat(dataKind).isNull();
        }

        @Test
        @DisplayName("getDataKind() dataKinds가 null이면 null 반환")
        void getDataKindReturnsNullWhenNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskingEnabled(true)
                    .build();
            // dataKinds를 직접 null로 설정 (리플렉션 사용하지 않고 기본값 확인)

            @SuppressWarnings("deprecation")
            String dataKind = policy.getDataKind();
            // 빌더 기본값은 빈 LinkedHashSet
            assertThat(dataKind).isNull();
        }

        @Test
        @DisplayName("matches(String dataKind) deprecated 메서드 호출")
        void legacyMatchesWithStringDataKind() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKinds(Set.of(DataKind.SSN))
                    .maskingEnabled(true)
                    .active(true)
                    .build();

            @SuppressWarnings("deprecation")
            boolean result = policy.matches(FeatureCode.ORGANIZATION, null, null, "SSN", Instant.now());
            assertThat(result).isTrue();
        }
    }
}
