package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.policy.MaskingMatch;

@DisplayName("Builder 레거시 메서드 커버리지")
@SuppressWarnings("deprecation")
class BuilderLegacyCoverageTest {

    @Nested
    @DisplayName("MaskingTarget.MaskingTargetBuilder 레거시 메서드")
    class MaskingTargetBuilderLegacy {

        @Test
        @DisplayName("dataKind(String) - 문자열로 DataKind 설정")
        void dataKindFromString() {
            MaskingTarget target = MaskingTarget.builder()
                    .dataKind("SSN")
                    .build();
            assertThat(target.getDataKind()).isEqualTo(DataKind.SSN);
        }

        @Test
        @DisplayName("dataKind(String) - null 처리")
        void dataKindFromStringNull() {
            MaskingTarget target = MaskingTarget.builder()
                    .dataKind((String) null)
                    .build();
            assertThat(target.getDataKind()).isEqualTo(DataKind.DEFAULT);
        }

        @Test
        @DisplayName("forceUnmaskKindsFromStrings - Set<String> → Set<DataKind>")
        void forceUnmaskKindsFromStrings() {
            MaskingTarget target = MaskingTarget.builder()
                    .forceUnmaskKindsFromStrings(Set.of("SSN", "PHONE"))
                    .build();
            assertThat(target.getForceUnmaskKinds()).containsExactlyInAnyOrder(DataKind.SSN, DataKind.PHONE);
        }

        @Test
        @DisplayName("forceUnmaskKindsFromStrings - null 처리")
        void forceUnmaskKindsFromStringsNull() {
            MaskingTarget target = MaskingTarget.builder()
                    .forceUnmaskKindsFromStrings(null)
                    .build();
            assertThat(target.getForceUnmaskKinds()).isEmpty();
        }

        @Test
        @DisplayName("forceUnmaskKindsFromStrings - 빈 Set 처리")
        void forceUnmaskKindsFromStringsEmpty() {
            MaskingTarget target = MaskingTarget.builder()
                    .forceUnmaskKindsFromStrings(Set.of())
                    .build();
            assertThat(target.getForceUnmaskKinds()).isEmpty();
        }

        @Test
        @DisplayName("getDataKindString() - DataKind를 String으로 반환")
        void getDataKindString() {
            MaskingTarget target = MaskingTarget.builder()
                    .dataKind(DataKind.SSN)
                    .build();
            assertThat(target.getDataKindString()).isEqualTo("SSN");
        }

        @Test
        @DisplayName("getDataKindString() - null DataKind 처리")
        void getDataKindStringNull() {
            MaskingTarget target = MaskingTarget.builder().build();
            assertThat(target.getDataKindString()).isNull();
        }

        @Test
        @DisplayName("maskRule과 maskParams 설정")
        void maskRuleAndParams() {
            MaskingTarget target = MaskingTarget.builder()
                    .dataKind(DataKind.SSN)
                    .maskRule("PARTIAL")
                    .maskParams("{\"start\": 3}")
                    .build();
            assertThat(target.getMaskRule()).isEqualTo("PARTIAL");
            assertThat(target.getMaskParams()).isEqualTo("{\"start\": 3}");
        }

        @Test
        @DisplayName("toBuilder() 테스트")
        void toBuilderTest() {
            MaskingTarget original = MaskingTarget.builder()
                    .dataKind(DataKind.SSN)
                    .forceUnmask(false)
                    .build();
            MaskingTarget modified = original.toBuilder()
                    .forceUnmask(true)
                    .build();
            assertThat(modified.getDataKind()).isEqualTo(DataKind.SSN);
            assertThat(modified.isForceUnmask()).isTrue();
        }
    }

    @Nested
    @DisplayName("MaskingMatch.MaskingMatchBuilder 레거시 메서드")
    class MaskingMatchBuilderLegacy {

        @Test
        @DisplayName("dataKind(String) - 문자열로 DataKind 설정")
        void dataKindFromString() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKind("SSN")
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKinds()).containsExactly(DataKind.SSN);
        }

        @Test
        @DisplayName("dataKind(String) - null 처리")
        void dataKindFromStringNull() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKind((String) null)
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKinds()).isEmpty();
        }

        @Test
        @DisplayName("dataKind(String) - 빈 문자열 처리")
        void dataKindFromStringBlank() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKind("   ")
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKinds()).isEmpty();
        }

        @Test
        @DisplayName("maskRule(String) - NONE → maskingEnabled=false")
        void maskRuleNone() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskRule("NONE")
                    .build();
            assertThat(match.isMaskingEnabled()).isFalse();
        }

        @Test
        @DisplayName("maskRule(String) - PARTIAL → maskingEnabled=true")
        void maskRulePartial() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskRule("PARTIAL")
                    .build();
            assertThat(match.isMaskingEnabled()).isTrue();
        }

        @Test
        @DisplayName("maskParams(String) - 무시됨")
        void maskParamsIgnored() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskParams("{\"param\": 1}")
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getMaskParams()).isNull(); // 항상 null 반환
        }

        @Test
        @DisplayName("dataKindsFromStrings - Set<String> → Set<DataKind>")
        void dataKindsFromStrings() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKindsFromStrings(Set.of("SSN", "PHONE", "EMAIL"))
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKinds()).containsExactlyInAnyOrder(DataKind.SSN, DataKind.PHONE, DataKind.EMAIL);
        }

        @Test
        @DisplayName("dataKindsFromStrings - null 처리")
        void dataKindsFromStringsNull() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKindsFromStrings(null)
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKinds()).isEmpty();
        }

        @Test
        @DisplayName("dataKindsFromStrings - 빈 Set 처리")
        void dataKindsFromStringsEmpty() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKindsFromStrings(Set.of())
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKinds()).isEmpty();
        }

        @Test
        @DisplayName("getDataKind() - 첫 번째 DataKind 반환")
        void getDataKindLegacy() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKinds(Set.of(DataKind.SSN))
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKind()).isEqualTo("SSN");
        }

        @Test
        @DisplayName("getDataKind() - null/빈 Set인 경우 null 반환")
        void getDataKindLegacyNull() {
            MaskingMatch match1 = MaskingMatch.builder()
                    .dataKinds(null)
                    .maskingEnabled(true)
                    .build();
            assertThat(match1.getDataKind()).isNull();

            MaskingMatch match2 = MaskingMatch.builder()
                    .dataKinds(Set.of())
                    .maskingEnabled(true)
                    .build();
            assertThat(match2.getDataKind()).isNull();
        }

        @Test
        @DisplayName("getMaskRule() - maskingEnabled에 따른 반환")
        void getMaskRuleLegacy() {
            MaskingMatch enabled = MaskingMatch.builder()
                    .maskingEnabled(true)
                    .build();
            assertThat(enabled.getMaskRule()).isEqualTo("PARTIAL");

            MaskingMatch disabled = MaskingMatch.builder()
                    .maskingEnabled(false)
                    .build();
            assertThat(disabled.getMaskRule()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("appliesTo() - dataKinds 매칭 확인")
        void appliesToMethod() {
            MaskingMatch matchWithKinds = MaskingMatch.builder()
                    .dataKinds(Set.of(DataKind.SSN, DataKind.PHONE))
                    .maskingEnabled(true)
                    .build();
            assertThat(matchWithKinds.appliesTo(DataKind.SSN)).isTrue();
            assertThat(matchWithKinds.appliesTo(DataKind.EMAIL)).isFalse();
            assertThat(matchWithKinds.appliesTo(null)).isFalse();

            // 빈 Set은 모든 종류에 적용
            MaskingMatch matchEmpty = MaskingMatch.builder()
                    .dataKinds(Set.of())
                    .maskingEnabled(true)
                    .build();
            assertThat(matchEmpty.appliesTo(DataKind.SSN)).isTrue();
            assertThat(matchEmpty.appliesTo(DataKind.EMAIL)).isTrue();

            // null dataKinds도 모든 종류에 적용
            MaskingMatch matchNull = MaskingMatch.builder()
                    .dataKinds(null)
                    .maskingEnabled(true)
                    .build();
            assertThat(matchNull.appliesTo(DataKind.CARD_NO)).isTrue();
        }
    }
}
