package com.example.admin.rowaccesspolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRepository;
import com.example.common.policy.RowAccessMatch;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;

@DisplayName("RowAccessPolicyService")
class RowAccessPolicyServiceTest {

    RowAccessPolicyRepository repository = Mockito.mock(RowAccessPolicyRepository.class);
    RowAccessPolicyService service = new RowAccessPolicyService(repository);

    private RowAccessPolicyRoot createRoot() {
        return RowAccessPolicyRoot.create(OffsetDateTime.now());
    }

    private RowAccessPolicy createPolicy(
            RowAccessPolicyRoot root,
            FeatureCode featureCode,
            ActionCode actionCode,
            String permGroupCode,
            String orgGroupCode,
            RowScope rowScope,
            int priority,
            boolean active,
            Instant effectiveFrom,
            Instant effectiveTo
    ) {
        return RowAccessPolicy.create(
                root,
                1,
                "Test Policy",
                "Test Description",
                featureCode,
                actionCode,
                permGroupCode,
                orgGroupCode,
                rowScope,
                priority,
                active,
                effectiveFrom,
                effectiveTo,
                ChangeAction.CREATE,
                null,
                "tester",
                "테스터",
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("evaluate 메서드는")
    class Evaluate {

        @Test
        @DisplayName("유효한 정책이 있으면 RowAccessMatch를 반환한다")
        void returnsMatchWhenEffective() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(
                    root, FeatureCode.ORGANIZATION, ActionCode.READ, "PG", null,
                    RowScope.OWN, 1, true, now.minusSeconds(10), now.plusSeconds(10)
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

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
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of());

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, Instant.now());

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("여러 정책 중 우선순위가 가장 높은 정책이 반환된다")
        void returnsHighestPriorityPolicy() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy lowPriority = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    RowScope.ALL, 100, true, null, null
            );
            RowAccessPolicy highPriority = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    RowScope.OWN, 1, true, null, null
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(lowPriority, highPriority));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, now);

            assertThat(match).isPresent();
            assertThat(match.get().getRowScope()).isEqualTo(RowScope.OWN);
        }

        @Test
        @DisplayName("유효 기간이 지난 정책은 무시된다")
        void skipsExpiredPolicy() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy expired = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    RowScope.ALL, 1, true, null, now.minusSeconds(1)
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(expired));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("아직 유효하지 않은 정책은 무시된다")
        void skipsFuturePolicy() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy future = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    RowScope.ALL, 1, true, now.plusSeconds(100), null
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(future));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되는 정책을 찾는다")
        void matchesOrgGroupCode() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, "ORG_A",
                    RowScope.ORG, 1, true, null, null
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), now);

            assertThat(match).isPresent();
            assertThat(match.get().getRowScope()).isEqualTo(RowScope.ORG);
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되지 않으면 무시된다")
        void skipsNonMatchingOrgGroup() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, "ORG_X",
                    RowScope.ORG, 1, true, null, null
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("now가 null이면 현재 시간을 사용한다")
        void usesCurrentTimeWhenNowIsNull() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    RowScope.ALL, 1, true, null, null
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", null, null, null, null);

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("소문자 featureCode도 파싱된다")
        void parsesLowercaseFeatureCode() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    RowScope.ALL, 1, true, null, null
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("organization", null, null, null, Instant.now());

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("잘못된 actionCode는 null로 처리된다")
        void invalidActionCodeTreatedAsNull() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    RowScope.ALL, 1, true, null, null
            );
            given(repository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<RowAccessMatch> match = service.evaluate("ORGANIZATION", "INVALID_ACTION", null, null, Instant.now());

            assertThat(match).isPresent();
        }
    }
}
