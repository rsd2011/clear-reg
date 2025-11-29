package com.example.admin.maskingpolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.maskingpolicy.domain.MaskingPolicyRoot;
import com.example.admin.maskingpolicy.domain.MaskingPolicy;
import com.example.admin.maskingpolicy.repository.MaskingPolicyRepository;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.masking.DataKind;
import com.example.common.policy.MaskingMatch;
import com.example.common.version.ChangeAction;

@DisplayName("MaskingPolicyService")
class MaskingPolicyServiceTest {

    MaskingPolicyRepository versionRepository = Mockito.mock(MaskingPolicyRepository.class);
    MaskingPolicyService service = new MaskingPolicyService(versionRepository);

    private MaskingPolicyRoot createRoot() {
        return MaskingPolicyRoot.createWithCode("TEST_POLICY", OffsetDateTime.now());
    }

    private MaskingPolicy createVersion(MaskingPolicyRoot root,
                                               FeatureCode featureCode,
                                               ActionCode actionCode,
                                               String permGroupCode,
                                               String orgGroupCode,
                                               Set<DataKind> dataKinds,
                                               Boolean maskingEnabled,
                                               Boolean auditEnabled,
                                               Integer priority,
                                               Instant effectiveFrom,
                                               Instant effectiveTo) {
        OffsetDateTime now = OffsetDateTime.now();
        return MaskingPolicy.create(
                root,
                1,
                "Test Policy",
                "Test Description",
                featureCode,
                actionCode,
                permGroupCode,
                orgGroupCode,
                dataKinds,
                maskingEnabled,
                auditEnabled,
                priority,
                true,
                effectiveFrom,
                effectiveTo,
                ChangeAction.CREATE,
                null,
                "tester",
                "tester",
                now
        );
    }

    @Nested
    @DisplayName("evaluate 메서드는")
    class Evaluate {

        @Test
        @DisplayName("유효한 정책이 있으면 MaskingMatch를 반환한다")
        void returnsMatchWhenEffective() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy version = createVersion(
                    root,
                    FeatureCode.ORGANIZATION,
                    ActionCode.READ,
                    "PG",
                    null,
                    Set.of(DataKind.SSN),
                    true,
                    true,
                    1,
                    now.minusSeconds(10),
                    now.plusSeconds(10)
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(version));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", "READ", "PG", List.of(), DataKind.SSN, now);

            assertThat(match).isPresent();
            assertThat(match.get().isMaskingEnabled()).isTrue();
            assertThat(match.get().getDataKinds()).contains(DataKind.SSN);
            assertThat(match.get().isAuditEnabled()).isTrue();
        }

        @Test
        @DisplayName("featureCode가 null이거나 빈 문자열이면 empty를 반환한다")
        void returnsEmptyWhenFeatureCodeInvalid() {
            Optional<MaskingMatch> match1 = service.evaluate(null, null, null, null, (DataKind) null, Instant.now());
            Optional<MaskingMatch> match2 = service.evaluate("", null, null, null, (DataKind) null, Instant.now());
            Optional<MaskingMatch> match3 = service.evaluate("  ", null, null, null, (DataKind) null, Instant.now());

            assertThat(match1).isEmpty();
            assertThat(match2).isEmpty();
            assertThat(match3).isEmpty();
        }

        @Test
        @DisplayName("잘못된 featureCode면 empty를 반환한다")
        void returnsEmptyWhenFeatureCodeUnknown() {
            Optional<MaskingMatch> match = service.evaluate("INVALID_FEATURE", null, null, null, (DataKind) null, Instant.now());

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("매칭되는 정책이 없으면 empty를 반환한다")
        void returnsEmptyWhenNoMatch() {
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of());

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, (DataKind) null, Instant.now());

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("여러 정책 중 우선순위가 가장 높은 정책이 반환된다")
        void returnsHighestPriorityPolicy() {
            Instant now = Instant.now();
            MaskingPolicyRoot root1 = createRoot();
            MaskingPolicyRoot root2 = createRoot();
            MaskingPolicy lowPriority = createVersion(
                    root1, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(), false, false, 100, null, null
            );
            MaskingPolicy highPriority = createVersion(
                    root2, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(lowPriority, highPriority));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, (DataKind) null, now);

            assertThat(match).isPresent();
            assertThat(match.get().isMaskingEnabled()).isTrue();
        }

        @Test
        @DisplayName("유효 기간이 지난 정책은 무시된다")
        void skipsExpiredPolicy() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy expired = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(), true, false, 1, null, now.minusSeconds(1)
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(expired));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, (DataKind) null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("아직 유효하지 않은 정책은 무시된다")
        void skipsFuturePolicy() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy future = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(), true, false, 1, now.plusSeconds(100), null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(future));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, (DataKind) null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("dataKind가 매칭되는 정책을 찾는다")
        void matchesDataKind() {
            Instant now = Instant.now();
            MaskingPolicyRoot root1 = createRoot();
            MaskingPolicyRoot root2 = createRoot();
            MaskingPolicy ssnPolicy = createVersion(
                    root1, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(DataKind.SSN), true, false, 1, null, null
            );
            MaskingPolicy phonePolicy = createVersion(
                    root2, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(DataKind.PHONE), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(ssnPolicy, phonePolicy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, DataKind.SSN, now);

            assertThat(match).isPresent();
            assertThat(match.get().getDataKinds()).contains(DataKind.SSN);
        }

        @Test
        @DisplayName("dataKinds가 비어있는 정책은 모든 종류에 매칭된다")
        void emptyDataKindsMatchesAll() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy generalPolicy = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(generalPolicy));

            Optional<MaskingMatch> match1 = service.evaluate("ORGANIZATION", null, null, null, DataKind.SSN, now);
            Optional<MaskingMatch> match2 = service.evaluate("ORGANIZATION", null, null, null, DataKind.PHONE, now);
            Optional<MaskingMatch> match3 = service.evaluate("ORGANIZATION", null, null, null, (DataKind) null, now);

            assertThat(match1).isPresent();
            assertThat(match2).isPresent();
            assertThat(match3).isPresent();
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되는 정책을 찾는다")
        void matchesOrgGroupCode() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy policy = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, "ORG_A",
                    Set.of(), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), (DataKind) null, now);

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("orgGroupCode가 매칭되지 않으면 무시된다")
        void skipsNonMatchingOrgGroup() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy policy = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, "ORG_X",
                    Set.of(), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, List.of("ORG_A", "ORG_B"), (DataKind) null, now);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("now가 null이면 현재 시간을 사용한다")
        void usesCurrentTimeWhenNowIsNull() {
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy policy = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, (DataKind) null, null);

            assertThat(match).isPresent();
        }

        @Test
        @DisplayName("auditEnabled가 false인 정책")
        void auditEnabledFalse() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy policy = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            Optional<MaskingMatch> match = service.evaluate("ORGANIZATION", null, null, null, (DataKind) null, now);

            assertThat(match).isPresent();
            assertThat(match.get().isAuditEnabled()).isFalse();
        }

        @Test
        @DisplayName("여러 dataKinds를 가진 정책이 매칭된다")
        void matchesMultipleDataKinds() {
            Instant now = Instant.now();
            MaskingPolicyRoot root = createRoot();
            MaskingPolicy policy = createVersion(
                    root, FeatureCode.ORGANIZATION, null, null, null,
                    Set.of(DataKind.SSN, DataKind.PHONE, DataKind.EMAIL), true, false, 1, null, null
            );
            given(versionRepository.findAllCurrentActiveVersions()).willReturn(List.of(policy));

            assertThat(service.evaluate("ORGANIZATION", null, null, null, DataKind.SSN, now)).isPresent();
            assertThat(service.evaluate("ORGANIZATION", null, null, null, DataKind.PHONE, now)).isPresent();
            assertThat(service.evaluate("ORGANIZATION", null, null, null, DataKind.EMAIL, now)).isPresent();
            assertThat(service.evaluate("ORGANIZATION", null, null, null, DataKind.CARD_NO, now)).isEmpty();
        }
    }
}
