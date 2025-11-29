package com.example.admin.rowaccesspolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRepository;
import com.example.common.policy.RowAccessMatch;
import com.example.common.security.RowScope;

@DisplayName("RowAccessPolicyService")
class RowAccessPolicyServiceTest {

    RowAccessPolicyRepository repository = Mockito.mock(RowAccessPolicyRepository.class);
    RowAccessPolicyService service = new RowAccessPolicyService(repository);

    @Nested
    @DisplayName("evaluate 메서드는")
    class Evaluate {

        @Test
        @DisplayName("유효한 정책이 있으면 RowAccessMatch를 반환한다")
        void returnsMatchWhenEffective() {
            Instant now = Instant.now();
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .permGroupCode("PG")
                    .rowScope(RowScope.OWN)
                    .priority(1)
                    .active(true)
                    .effectiveFrom(now.minusSeconds(10))
                    .effectiveTo(now.plusSeconds(10))
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", "READ", "PG", List.of(), now);

            assertThat(match).isPresent();
            assertThat(match.get().getRowScope()).isEqualTo(RowScope.OWN);
        }

        @Test
        @DisplayName("featureCode가 null이거나 빈 문자열이면 empty를 반환한다")
        void returnsEmptyWhenFeatureCodeInvalid() {
            Optional<RowAccessMatch> match1 = service.evaluate(null, null, null, null, Instant.now());
            Optional<RowAccessMatch> match2 = service.evaluate("", null, null, null, Instant.now());
            Optional<RowAccessMatch> match3 = service.evaluate("  ", null, null, null, Instant.now());

            assertThat(match1).isEmpty();
            assertThat(match2).isEmpty();
            assertThat(match3).isEmpty();
        }

        @Test
        @DisplayName("잘못된 featureCode면 empty를 반환한다")
        void returnsEmptyWhenFeatureCodeUnknown() {
            Optional<RowAccessMatch> match = service.evaluate("INVALID_FEATURE", null, null, null, Instant.now());

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("매칭되는 정책이 없으면 empty를 반환한다")
        void returnsEmptyWhenNoMatch() {
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of());

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, Instant.now());

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("여러 정책 중 우선순위가 가장 높은 정책이 반환된다")
        void returnsHighestPriorityPolicy() {
            Instant now = Instant.now();
            RowAccessPolicy lowPriority = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .priority(100)
                    .active(true)
                    .build();
            RowAccessPolicy highPriority = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.OWN)
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(lowPriority, highPriority));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, now);

            assertThat(match).isPresent();
            assertThat(match.get().getRowScope()).isEqualTo(RowScope.OWN);
        }

        @Test
        @DisplayName("유효 기간이 지난 정책은 무시된다")
        void skipsExpiredPolicy() {
            Instant now = Instant.now();
            RowAccessPolicy expired = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .priority(1)
                    .active(true)
                    .effectiveTo(now.minusSeconds(1))
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(expired));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("아직 유효하지 않은 정책은 무시된다")
        void skipsFuturePolicy() {
            Instant now = Instant.now();
            RowAccessPolicy future = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .priority(1)
                    .active(true)
                    .effectiveFrom(now.plusSeconds(100))
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(future));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되는 정책을 찾는다")
        void matchesOrgGroupCode() {
            Instant now = Instant.now();
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .orgGroupCode("ORG_A")
                    .rowScope(RowScope.ORG)
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), now);

            assertThat(match).isPresent();
            assertThat(match.get().getRowScope()).isEqualTo(RowScope.ORG);
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되지 않으면 무시된다")
        void skipsNonMatchingOrgGroup() {
            Instant now = Instant.now();
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .orgGroupCode("ORG_X")
                    .rowScope(RowScope.ORG)
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("now가 null이면 현재 시간을 사용한다")
        void usesCurrentTimeWhenNowIsNull() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, null);

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("소문자 featureCode도 파싱된다")
        void parsesLowercaseFeatureCode() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("organization", null, null, null, Instant.now());

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("잘못된 actionCode는 null로 처리된다")
        void invalidActionCodeTreatedAsNull() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .priority(1)
                    .active(true)
                    .build();
            given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", "INVALID_ACTION", null, null, Instant.now());

            assertThat(match).isPresent();
        }
    }
}
