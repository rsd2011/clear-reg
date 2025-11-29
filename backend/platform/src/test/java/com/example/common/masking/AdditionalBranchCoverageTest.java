package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.MaskingMatch;

@DisplayName("추가 브랜치 커버리지")
class AdditionalBranchCoverageTest {

    @Test
    @DisplayName("MaskRule/MaskingFunctions of/null/invalid 경로")
    void maskRuleBranches() {
        assertThat(MaskRule.of(null)).isEqualTo(MaskRule.NONE);
        assertThat(MaskRule.of("hash")).isEqualTo(MaskRule.HASH);
        assertThat(MaskRule.of("invalid")).isEqualTo(MaskRule.NONE);
        assertThat(MaskRule.of("tokenize")).isEqualTo(MaskRule.TOKENIZE);

        DataPolicyMatch emptyMatch = null;
        assertThat(MaskingFunctions.masker(emptyMatch).apply("VALUE")).isEqualTo("VALUE");

        DataPolicyMatch fullMatch = DataPolicyMatch.builder().maskRule("FULL").maskParams(null).build();
        assertThat(MaskingFunctions.masker(fullMatch).apply("123456")).isEqualTo("[MASKED]");

        DataPolicyMatch partial = DataPolicyMatch.builder().maskRule("PARTIAL").build();
        assertThat(MaskingFunctions.masker(partial).apply("12345678")).startsWith("12");
        assertThat(MaskingFunctions.masker(partial).apply("123")).isEqualTo("***");
        DataPolicyMatch hash = DataPolicyMatch.builder().maskRule("HASH").build();
        assertThat(MaskingFunctions.masker(hash).apply("abc")).hasSize(64);
        DataPolicyMatch tokenize = DataPolicyMatch.builder().maskRule("TOKENIZE").build();
        assertThat(MaskingFunctions.masker(tokenize).apply("abc")).contains("-");

        // null value
        assertThat(MaskRuleProcessor.apply("FULL", null, null)).isNull();
    }

    @Test
    @DisplayName("MaskingMatch를 사용한 MaskingFunctions 테스트")
    void maskingFunctionsWithMaskingMatch() {
        MaskingMatch emptyMatch = null;
        assertThat(MaskingFunctions.masker(emptyMatch).apply("VALUE")).isEqualTo("VALUE");

        MaskingMatch fullMatch = MaskingMatch.builder().maskRule("FULL").maskParams(null).build();
        assertThat(MaskingFunctions.masker(fullMatch).apply("123456")).isEqualTo("[MASKED]");

        MaskingMatch partial = MaskingMatch.builder().maskRule("PARTIAL").build();
        assertThat(MaskingFunctions.masker(partial).apply("12345678")).startsWith("12");

        MaskingMatch hash = MaskingMatch.builder().maskRule("HASH").build();
        assertThat(MaskingFunctions.masker(hash).apply("abc")).hasSize(64);

        MaskingMatch tokenize = MaskingMatch.builder().maskRule("TOKENIZE").build();
        assertThat(MaskingFunctions.masker(tokenize).apply("abc")).contains("-");
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
        String out = auditService.render(maskable, MaskingTarget.builder().forceUnmask(true).subjectType(SubjectType.CUSTOMER_INDIVIDUAL).dataKind("RRN").build(), "field");
        assertThat(out).isEqualTo("RAW");
        assertThat(events).hasSize(1);
    }
}
