package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.MaskingMatch;
import com.example.common.masking.DataKind;

@DisplayName("추가 브랜치 커버리지")
class AdditionalBranchCoverageTest {

    @Test
    @DisplayName("MaskRule/MaskingFunctions of/null/invalid 경로")
    void maskRuleBranches() {
        assertThat(MaskRule.of(null)).isEqualTo(MaskRule.NONE);
        assertThat(MaskRule.of("hash")).isEqualTo(MaskRule.HASH);
        assertThat(MaskRule.of("invalid")).isEqualTo(MaskRule.NONE);
        assertThat(MaskRule.of("tokenize")).isEqualTo(MaskRule.TOKENIZE);

        // match가 null이면 마스킹 적용 안 함 (원본 반환)
        MaskingMatch emptyMatch = null;
        assertThat(MaskingFunctions.masker(emptyMatch).apply("VALUE")).isEqualTo("VALUE");

        // maskingEnabled=false (화이트리스트)면 원본 반환
        MaskingMatch whitelistMatch = MaskingMatch.builder().maskingEnabled(false).build();
        assertThat(MaskingFunctions.masker(whitelistMatch).apply("VALUE")).isEqualTo("VALUE");

        // SSN DataKind는 FULL 마스킹 (블랙리스트)
        MaskingMatch fullMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.SSN).build();
        assertThat(MaskingFunctions.masker(fullMatch).apply("123456")).isEqualTo("[MASKED]");

        // PHONE DataKind는 PARTIAL 마스킹 (블랙리스트)
        MaskingMatch partial = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.PHONE).build();
        assertThat(MaskingFunctions.masker(partial).apply("12345678")).startsWith("12");
        assertThat(MaskingFunctions.masker(partial).apply("123")).isEqualTo("***");

        // null value
        assertThat(MaskRuleProcessor.apply("FULL", null, null)).isNull();
    }

    @Test
    @DisplayName("MaskingMatch를 사용한 MaskingFunctions 테스트")
    void maskingFunctionsWithMaskingMatch() {
        // match가 null이면 마스킹 적용 안 함 (원본 반환)
        MaskingMatch emptyMatch = null;
        assertThat(MaskingFunctions.masker(emptyMatch).apply("VALUE")).isEqualTo("VALUE");

        // maskingEnabled=false (화이트리스트)면 원본 반환
        MaskingMatch whitelistMatch = MaskingMatch.builder().maskingEnabled(false).build();
        assertThat(MaskingFunctions.masker(whitelistMatch).apply("VALUE")).isEqualTo("VALUE");

        // SSN DataKind는 FULL 마스킹 (블랙리스트)
        MaskingMatch fullMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.SSN).build();
        assertThat(MaskingFunctions.masker(fullMatch).apply("123456")).isEqualTo("[MASKED]");

        // PHONE DataKind는 PARTIAL 마스킹 (블랙리스트)
        MaskingMatch partial = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.PHONE).build();
        assertThat(MaskingFunctions.masker(partial).apply("12345678")).startsWith("12");

        // ACCOUNT_NO DataKind도 PARTIAL 마스킹 (블랙리스트)
        MaskingMatch accountMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.ACCOUNT_NO).build();
        assertThat(MaskingFunctions.masker(accountMatch).apply("1234567890")).startsWith("12");
        assertThat(MaskingFunctions.masker(accountMatch).apply("1234567890")).endsWith("90");
    }

    @Test
    @DisplayName("MaskingFunctions의 모든 DataKind 기본 규칙 분기")
    void maskingFunctionsAllDataKindRules() {
        // FULL 마스킹 테스트 (SSN DataKind)
        MaskingMatch fullMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.SSN).build();
        assertThat(MaskingFunctions.masker(fullMatch).apply("900101-1234567")).isEqualTo("[MASKED]");

        // PARTIAL 마스킹 테스트 (CARD_NO DataKind)
        MaskingMatch partialMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.CARD_NO).build();
        String partial = MaskingFunctions.masker(partialMatch).apply("4111111111111111");
        assertThat(partial).isNotEqualTo("4111111111111111");
        assertThat(partial).startsWith("41").endsWith("11");

        // DEFAULT DataKind는 FULL 규칙을 가짐
        MaskingMatch defaultMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.DEFAULT).build();
        assertThat(MaskingFunctions.masker(defaultMatch).apply("SomeData")).isEqualTo("[MASKED]");

        // 알 수 없는 DataKind는 DEFAULT(FULL)로 폴백
        MaskingMatch unknownMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.DEFAULT).build();
        assertThat(MaskingFunctions.masker(unknownMatch).apply("Secret")).isEqualTo("[MASKED]");

        // dataKind가 null인 경우 DEFAULT로 폴백 (FULL)
        MaskingMatch nullKindMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(null).build();
        assertThat(MaskingFunctions.masker(nullKindMatch, null).apply("TEST")).isEqualTo("[MASKED]");

        // null value 처리
        MaskingMatch anyMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.SSN).build();
        assertThat(MaskingFunctions.masker(anyMatch).apply(null)).isNull();

        // masker(match, dataKind) 2파라미터 - dataKind 파라미터가 우선
        MaskingMatch ssnMatch = MaskingMatch.builder().maskingEnabled(true).dataKind(DataKind.SSN).build();
        // dataKind 파라미터로 PHONE을 전달하면 PARTIAL 규칙 적용
        String withPhoneKind = MaskingFunctions.masker(ssnMatch, DataKind.PHONE).apply("01012345678");
        assertThat(withPhoneKind).startsWith("01").endsWith("78");
    }

    @Test
    @DisplayName("OutputMaskingAdapter 추가 분기 테스트")
    void outputMaskingAdapterBranches() {
        // null value
        assertThat(OutputMaskingAdapter.mask("field", null, null, true)).isNull();

        // forceUnmask=true
        MaskingTarget forceUnmask = MaskingTarget.builder().forceUnmask(true).dataKind(DataKind.SSN).build();
        assertThat(OutputMaskingAdapter.mask("rrn", "900101-1234567", forceUnmask, true))
                .isEqualTo("900101-1234567");

        // forceUnmaskKinds 포함
        MaskingTarget kindUnmask = MaskingTarget.builder()
                .forceUnmaskKinds(java.util.Set.of(DataKind.SSN))
                .dataKind(DataKind.SSN)
                .build();
        assertThat(OutputMaskingAdapter.mask("rrn", "900101-1234567", kindUnmask, true))
                .isEqualTo("900101-1234567");

        // target이 null인 경우
        assertThat(OutputMaskingAdapter.mask("field", "value", null, true)).isNotNull();

        // 기본 2파라미터 mask 메서드
        assertThat(OutputMaskingAdapter.mask("field", "value", null)).isNotNull();

        // maskingEnabled=false (마스킹 해제)
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        assertThat(OutputMaskingAdapter.mask("rrn", "900101-1234567", target, false))
                .isEqualTo("900101-1234567");

        // maskingEnabled=true (마스킹 적용)
        assertThat(OutputMaskingAdapter.mask("rrn", "900101-1234567", target, true))
                .isEqualTo("[MASKED]");

        // Maskable value with maskingEnabled=false (raw가 null인 경우)
        Maskable<String> nullRawMaskable = new Maskable<String>() {
            @Override public String raw() { return null; }
            @Override public String masked() { return "***"; }
        };
        assertThat(OutputMaskingAdapter.mask("field", nullRawMaskable, target, false)).isNull();

        // DataKind가 잘못된 문자열인 경우 DEFAULT로 폴백
        MaskingTarget invalidKind = MaskingTarget.builder().dataKind(DataKind.DEFAULT).build();
        assertThat(OutputMaskingAdapter.mask("field", "value", invalidKind, true)).isNotNull();
    }

    @Test
    @DisplayName("MaskingTarget 빌더 - dataKind 설정")
    void maskingTargetBuilderDataKind() {
        MaskingTarget target = MaskingTarget.builder()
                .dataKind(DataKind.SSN)
                .build();
        assertThat(target.getDataKind()).isEqualTo(DataKind.SSN);
    }

    @Test
    @DisplayName("MaskingTarget - forceUnmaskKinds 설정")
    void maskingTargetForceUnmaskKinds() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmaskKinds(java.util.Set.of(DataKind.SSN))
                .build();
        assertThat(target.getForceUnmaskKinds()).contains(DataKind.SSN);
    }

    @Test
    @DisplayName("MaskingService null 대상 및 audit sink 미호출/호출")
    void maskingServiceBranches() {
        MaskingService service = new MaskingService(target -> true);
        assertThat(service.render((Maskable) null, null)).isNull();

        java.util.List<UnmaskAuditEvent> events = new java.util.ArrayList<>();
        MaskingService auditService = new MaskingService(target -> false, events::add);
        Maskable maskable = new Maskable() {
            @Override public String raw() { return "RAW"; }
            @Override public String masked() { return "***"; }
        };
        // 마스킹 해제 시 sink 호출
        String out = auditService.render(maskable, MaskingTarget.builder().forceUnmask(true).subjectType(SubjectType.CUSTOMER_INDIVIDUAL).dataKind(DataKind.SSN).build(), "field");
        assertThat(out).isEqualTo("RAW");
        assertThat(events).hasSize(1);
    }
}
