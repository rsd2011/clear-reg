package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.policy.MaskingMatch;

@DisplayName("Builder 레거시 메서드 커버리지")
class BuilderLegacyCoverageTest {

    @Nested
    @DisplayName("MaskingTarget 빌더 테스트")
    class MaskingTargetBuilderTests {

        @Test
        @DisplayName("dataKind(DataKind) - enum으로 DataKind 설정")
        void dataKindFromEnum() {
            MaskingTarget target = MaskingTarget.builder()
                    .dataKind(DataKind.SSN)
                    .build();
            assertThat(target.getDataKind()).isEqualTo(DataKind.SSN);
        }

        @Test
        @DisplayName("forceUnmaskKinds - Set<DataKind> 설정")
        void forceUnmaskKinds() {
            MaskingTarget target = MaskingTarget.builder()
                    .forceUnmaskKinds(Set.of(DataKind.SSN, DataKind.PHONE))
                    .build();
            assertThat(target.getForceUnmaskKinds()).containsExactlyInAnyOrder(DataKind.SSN, DataKind.PHONE);
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
    @DisplayName("MaskingMatch 빌더 테스트")
    class MaskingMatchBuilderTests {

        @Test
        @DisplayName("dataKinds(Set<DataKind>) - Set으로 DataKind 설정")
        void dataKindsFromSet() {
            MaskingMatch match = MaskingMatch.builder()
                    .dataKinds(Set.of(DataKind.SSN, DataKind.PHONE))
                    .maskingEnabled(true)
                    .build();
            assertThat(match.getDataKinds()).containsExactlyInAnyOrder(DataKind.SSN, DataKind.PHONE);
        }

        @Test
        @DisplayName("maskingEnabled=true 설정")
        void maskingEnabledTrue() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(true)
                    .build();
            assertThat(match.isMaskingEnabled()).isTrue();
        }

        @Test
        @DisplayName("maskingEnabled=false 설정")
        void maskingEnabledFalse() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(false)
                    .build();
            assertThat(match.isMaskingEnabled()).isFalse();
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
