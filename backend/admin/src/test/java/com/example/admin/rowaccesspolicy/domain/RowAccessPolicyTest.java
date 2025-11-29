package com.example.admin.rowaccesspolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("RowAccessPolicy 엔티티")
class RowAccessPolicyTest {

    private RowAccessPolicyRoot createRoot() {
        return RowAccessPolicyRoot.create(OffsetDateTime.now());
    }

    private RowAccessPolicy createPolicy(RowAccessPolicyRoot root, boolean active, Instant effectiveFrom, Instant effectiveTo) {
        return RowAccessPolicy.create(
                root,
                1,
                "Test Policy",
                "Test Description",
                FeatureCode.ORGANIZATION,
                null,
                null,
                null,
                RowScope.ALL,
                100,
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

    private RowAccessPolicy createPolicyWithAction(RowAccessPolicyRoot root, ActionCode actionCode, String permGroupCode) {
        return RowAccessPolicy.create(
                root,
                1,
                "Test Policy",
                "Test Description",
                FeatureCode.ORGANIZATION,
                actionCode,
                permGroupCode,
                null,
                RowScope.ALL,
                100,
                true,
                null,
                null,
                ChangeAction.CREATE,
                null,
                "tester",
                "테스터",
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("isEffectiveAt 메서드는")
    class IsEffectiveAt {

        @Test
        @DisplayName("active가 false면 false를 반환한다")
        void returnsFalseWhenInactive() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, false, null, null);

            assertThat(policy.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("effectiveFrom 이전이면 false를 반환한다")
        void returnsFalseBeforeEffectiveFrom() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, now.plusSeconds(100), null);

            assertThat(policy.isEffectiveAt(now)).isFalse();
        }

        @Test
        @DisplayName("effectiveTo 이후면 false를 반환한다")
        void returnsFalseAfterEffectiveTo() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, null, now.minusSeconds(100));

            assertThat(policy.isEffectiveAt(now)).isFalse();
        }

        @Test
        @DisplayName("유효 기간 내면 true를 반환한다")
        void returnsTrueWhenWithinRange() {
            Instant now = Instant.now();
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, now.minusSeconds(100), now.plusSeconds(100));

            assertThat(policy.isEffectiveAt(now)).isTrue();
        }

        @Test
        @DisplayName("effectiveFrom/To가 null이면 항상 유효하다")
        void returnsTrueWhenNoDateBounds() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, null, null);

            assertThat(policy.isEffectiveAt(Instant.now())).isTrue();
        }
    }

    @Nested
    @DisplayName("matches 메서드는")
    class Matches {

        @Test
        @DisplayName("featureCode가 다르면 false를 반환한다")
        void returnsFalseWhenFeatureMismatch() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, null, null);

            assertThat(policy.matches(FeatureCode.CUSTOMER, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasActionButParamNull() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicyWithAction(root, ActionCode.READ, null);

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoActionButParamHas() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicyWithAction(root, null, null);

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasPermGroupButParamNull() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicyWithAction(root, null, "ADMIN");

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoPermGroupButParamHas() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicyWithAction(root, null, null);

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, "ADMIN", Instant.now())).isFalse();
        }

        @Test
        @DisplayName("모든 조건이 매칭되면 true를 반환한다")
        void returnsTrueWhenAllMatch() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicyWithAction(root, ActionCode.READ, "ADMIN");

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, "ADMIN", Instant.now())).isTrue();
        }

        @Test
        @DisplayName("actionCode가 다르면 false를 반환한다")
        void returnsFalseWhenActionMismatch() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicyWithAction(root, ActionCode.READ, null);

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.UPDATE, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("permGroupCode가 다르면 false를 반환한다")
        void returnsFalseWhenPermGroupMismatch() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicyWithAction(root, null, "ADMIN");

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, "USER", Instant.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("버전 상태 관리")
    class VersionStatus {

        @Test
        @DisplayName("create로 생성하면 PUBLISHED 상태이다")
        void createReturnsPublishedStatus() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, null, null);

            assertThat(policy.getStatus()).isEqualTo(com.example.common.version.VersionStatus.PUBLISHED);
            assertThat(policy.isCurrent()).isTrue();
            assertThat(policy.isDraft()).isFalse();
        }

        @Test
        @DisplayName("createDraft로 생성하면 DRAFT 상태이다")
        void createDraftReturnsDraftStatus() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy draft = RowAccessPolicy.createDraft(
                    root,
                    2,
                    "Draft Policy",
                    "Draft Description",
                    FeatureCode.ORGANIZATION,
                    null,
                    null,
                    null,
                    RowScope.OWN,
                    50,
                    true,
                    null,
                    null,
                    "변경 이유",
                    "tester",
                    "테스터",
                    OffsetDateTime.now()
            );

            assertThat(draft.getStatus()).isEqualTo(com.example.common.version.VersionStatus.DRAFT);
            assertThat(draft.isDraft()).isTrue();
            assertThat(draft.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("close하면 validTo가 설정되고 HISTORICAL로 변경된다")
        void closeChangesToHistorical() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, null, null);
            OffsetDateTime closedAt = OffsetDateTime.now();

            policy.close(closedAt);

            assertThat(policy.getValidTo()).isEqualTo(closedAt);
            assertThat(policy.getStatus()).isEqualTo(com.example.common.version.VersionStatus.HISTORICAL);
            assertThat(policy.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("Draft를 publish하면 PUBLISHED로 변경된다")
        void publishChangesToPublished() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy draft = RowAccessPolicy.createDraft(
                    root,
                    2,
                    "Draft Policy",
                    "Draft Description",
                    FeatureCode.ORGANIZATION,
                    null,
                    null,
                    null,
                    RowScope.OWN,
                    50,
                    true,
                    null,
                    null,
                    "변경 이유",
                    "tester",
                    "테스터",
                    OffsetDateTime.now()
            );
            OffsetDateTime publishedAt = OffsetDateTime.now();

            draft.publish(publishedAt);

            assertThat(draft.getStatus()).isEqualTo(com.example.common.version.VersionStatus.PUBLISHED);
            assertThat(draft.getChangeAction()).isEqualTo(ChangeAction.PUBLISH);
            assertThat(draft.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("PUBLISHED 상태에서 publish하면 예외가 발생한다")
        void publishThrowsWhenNotDraft() {
            RowAccessPolicyRoot root = createRoot();
            RowAccessPolicy policy = createPolicy(root, true, null, null);

            assertThatThrownBy(() -> policy.publish(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("초안 상태에서만 게시할 수 있습니다");
        }
    }

    @Test
    @DisplayName("기본값이 올바르게 설정된다")
    void defaultValues() {
        RowAccessPolicyRoot root = createRoot();
        RowAccessPolicy policy = RowAccessPolicy.create(
                root,
                1,
                "Test",
                null,
                FeatureCode.ORGANIZATION,
                null,
                null,
                null,
                RowScope.OWN,
                null,  // null이면 기본값 100
                true,
                null,
                null,
                ChangeAction.CREATE,
                null,
                "tester",
                "테스터",
                OffsetDateTime.now()
        );

        assertThat(policy.getPriority()).isEqualTo(100);
        assertThat(policy.isActive()).isTrue();
    }
}
