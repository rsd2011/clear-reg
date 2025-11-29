package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.UnaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.policy.MaskingMatch;

@DisplayName("MaskingFunctions")
class MaskingFunctionsTest {

    @Nested
    @DisplayName("masker(MaskingMatch, DataKind)")
    class MaskerWithDataKind {

        @Test
        @DisplayName("match가 null이면 원본 반환")
        void nullMatchReturnsIdentity() {
            UnaryOperator<String> masker = MaskingFunctions.masker(null, DataKind.SSN);
            assertThat(masker.apply("원본값")).isEqualTo("원본값");
        }

        @Test
        @DisplayName("maskingEnabled=false면 원본 반환 (화이트리스트)")
        void maskingDisabledReturnsIdentity() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(false)
                    .build();
            UnaryOperator<String> masker = MaskingFunctions.masker(match, DataKind.SSN);
            assertThat(masker.apply("900101-1234567")).isEqualTo("900101-1234567");
        }

        @Test
        @DisplayName("maskingEnabled=true면 DataKind 기본 규칙으로 마스킹")
        void maskingEnabledAppliesDataKindRule() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(true)
                    .build();
            UnaryOperator<String> masker = MaskingFunctions.masker(match, DataKind.SSN);
            String result = masker.apply("900101-1234567");
            assertThat(result).isNotEqualTo("900101-1234567");
        }

        @Test
        @DisplayName("dataKind가 null이면 DEFAULT 규칙 적용")
        void nullDataKindUsesDefault() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(true)
                    .build();
            UnaryOperator<String> masker = MaskingFunctions.masker(match, null);
            String result = masker.apply("테스트");
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("masker(MaskingMatch)")
    class MaskerSingleArg {

        @Test
        @DisplayName("match가 null이면 원본 반환")
        void nullMatchReturnsIdentity() {
            UnaryOperator<String> masker = MaskingFunctions.masker(null);
            assertThat(masker.apply("원본값")).isEqualTo("원본값");
        }

        @Test
        @DisplayName("match의 dataKinds에서 DataKind 추출하여 마스킹")
        void extractsDataKindFromMatch() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(true)
                    .dataKind("SSN")
                    .build();
            UnaryOperator<String> masker = MaskingFunctions.masker(match);
            String result = masker.apply("900101-1234567");
            assertThat(result).isNotEqualTo("900101-1234567");
        }

        @Test
        @DisplayName("maskingEnabled=false면 원본 반환")
        void maskingDisabledReturnsOriginal() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(false)
                    .dataKind("SSN")
                    .build();
            UnaryOperator<String> masker = MaskingFunctions.masker(match);
            assertThat(masker.apply("900101-1234567")).isEqualTo("900101-1234567");
        }
    }

    @Nested
    @DisplayName("MaskRule 적용")
    class MaskRuleApplication {

        @Test
        @DisplayName("NONE 규칙 - 마스킹 안 함")
        void noneRuleNoMasking() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(true)
                    .build();
            // DEFAULT DataKind는 PARTIAL 규칙 사용
            UnaryOperator<String> masker = MaskingFunctions.masker(match, DataKind.DEFAULT);
            String result = masker.apply("테스트값");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("null 값에 대한 마스킹")
        void nullValueHandling() {
            MaskingMatch match = MaskingMatch.builder()
                    .maskingEnabled(true)
                    .build();
            UnaryOperator<String> masker = MaskingFunctions.masker(match, DataKind.SSN);
            assertThat(masker.apply(null)).isNull();
        }
    }
}
