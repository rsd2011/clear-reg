package com.example.admin.maskingpolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.maskingpolicy.domain.MaskingPolicy;
import com.example.admin.maskingpolicy.repository.MaskingPolicyRepository;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.policy.MaskingMatch;

@DisplayName("MaskingPolicyService")
class MaskingPolicyServiceTest {

    MaskingPolicyRepository repository = Mockito.mock(MaskingPolicyRepository.class);
    MaskingPolicyService service = new MaskingPolicyService(repository);

    @Nested
    @DisplayName("evaluate 메서드는")
    class Evaluate {

        @Test
        @DisplayName("유효한 정책이 있으면 MaskingMatch를 반환한다")
        void returnsMatchWhenEffective() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .permGroupCode("PG")
                    .dataKind("SSN")
                    .maskRule("PARTIAL")
                    .maskParams("{\"visibleChars\":4}")
                    .auditEnabled(true)
                    .priority(1)
                    .active(true)
                    .effectiveFrom(now.minusSeconds(10))
                    .effectiveTo(now.plusSeconds(10))
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", "READ", "PG", List.of(), "SSN", now);

            assertThat(match).isPresent();
            assertThat(match.get().getMaskRule()).isEqualTo("PARTIAL");
            assertThat(match.get().getDataKind()).isEqualTo("SSN");
            assertThat(match.get().getMaskParams()).isEqualTo("{\"visibleChars\":4}");
            assertThat(match.get().isAuditEnabled()).isTrue();
        }

        @Test
        @DisplayName("featureCode가 null이거나 빈 문자열이면 empty를 반환한다")
        void returnsEmptyWhenFeatureCodeInvalid() {
            Optional<MaskingMatch> match1 = service.evaluate(null, null, null, null, null, Instant.now());
            Optional<MaskingMatch> match2 = service.evaluate("", null, null, null, null, Instant.now());
            Optional<MaskingMatch> match3 = service.evaluate("  ", null, null, null, null, Instant.now());

            assertThat(match1).isEmpty();
            assertThat(match2).isEmpty();
            assertThat(match3).isEmpty();
        }

        @Test
        @DisplayName("잘못된 featureCode면 empty를 반환한다")
        void returnsEmptyWhenFeatureCodeUnknown() {
            Optional<MaskingMatch> match = service.evaluate("INVALID_FEATURE", null, null, null, null, Instant.now());

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("매칭되는 정책이 없으면 empty를 반환한다")
        void returnsEmptyWhenNoMatch() {
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of());

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, null, Instant.now());

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("여러 정책 중 우선순위가 가장 높은 정책이 반환된다")
        void returnsHighestPriorityPolicy() {
            Instant now = Instant.now();
            MaskingPolicy lowPriority = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("NONE")
                    .priority(100)
                    .active(true)
                    .build();
            MaskingPolicy highPriority = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(lowPriority, highPriority));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, null, now);

            assertThat(match).isPresent();
            assertThat(match.get().getMaskRule()).isEqualTo("FULL");
        }

        @Test
        @DisplayName("유효 기간이 지난 정책은 무시된다")
        void skipsExpiredPolicy() {
            Instant now = Instant.now();
            MaskingPolicy expired = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .effectiveTo(now.minusSeconds(1))
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(expired));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("아직 유효하지 않은 정책은 무시된다")
        void skipsFuturePolicy() {
            Instant now = Instant.now();
            MaskingPolicy future = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .effectiveFrom(now.plusSeconds(100))
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(future));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("dataKind가 매칭되는 정책을 찾는다")
        void matchesDataKind() {
            Instant now = Instant.now();
            MaskingPolicy ssnPolicy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("SSN")
                    .maskRule("PARTIAL")
                    .priority(1)
                    .active(true)
                    .build();
            MaskingPolicy phonePolicy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("PHONE")
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(ssnPolicy, phonePolicy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, "SSN", now);

            assertThat(match).isPresent();
            assertThat(match.get().getMaskRule()).isEqualTo("PARTIAL");
        }

        @Test
        @DisplayName("dataKind가 null인 정책은 모든 종류에 매칭된다")
        void nullDataKindMatchesAll() {
            Instant now = Instant.now();
            MaskingPolicy generalPolicy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind(null)
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(generalPolicy));

            Optional<MaskingMatch> match1 = service.evaluate("ORGANIZATION", null, null, null, "SSN", now);
            Optional<MaskingMatch> match2 = service.evaluate("ORGANIZATION", null, null, null, "PHONE", now);
            Optional<MaskingMatch> match3 = service.evaluate("ORGANIZATION", null, null, null, null, now);

            assertThat(match1).isPresent();
            assertThat(match2).isPresent();
            assertThat(match3).isPresent();
        }

        @Test
        @DisplayName("dataKind 대소문자 무관 매칭")
        void dataKindCaseInsensitive() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .dataKind("SSN")
                    .maskRule("PARTIAL")
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, "ssn", now);

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되는 정책을 찾는다")
        void matchesOrgGroupCode() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .orgGroupCode("ORG_A")
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), null, now);

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되지 않으면 무시된다")
        void skipsNonMatchingOrgGroup() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .orgGroupCode("ORG_X")
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("now가 null이면 현재 시간을 사용한다")
        void usesCurrentTimeWhenNowIsNull() {
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, null, null);

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("auditEnabled가 false인 정책")
        void auditEnabledFalse() {
            Instant now = Instant.now();
            MaskingPolicy policy = MaskingPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .maskRule("FULL")
                    .auditEnabled(false)
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, null, now);

            assertThat(match).isPresent();
            assertThat(match.get().isAuditEnabled()).isFalse();
        }
    }
}
