package com.example.admin.maskingpolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.masking.DataKind;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("MaskingPolicyVersion 엔티티")
class MaskingPolicyVersionTest {

    @Nested
    @DisplayName("create 팩토리 메서드")
    class Create {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: create 호출 / Then: PUBLISHED 상태의 버전 생성")
        void createsPublishedVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책명", "설명",
                    FeatureCode.DRAFT, ActionCode.CREATE, "PERM_GROUP", "ORG_GROUP",
                    Set.of(DataKind.SSN, DataKind.PHONE), true, true, 50, true,
                    null, null,
                    ChangeAction.CREATE, "생성", "user", "사용자", now);

            assertThat(version.getRoot()).isEqualTo(root);
            assertThat(version.getVersion()).isEqualTo(1);
            assertThat(version.getName()).isEqualTo("정책명");
            assertThat(version.getDescription()).isEqualTo("설명");
            assertThat(version.getFeatureCode()).isEqualTo(FeatureCode.DRAFT);
            assertThat(version.getActionCode()).isEqualTo(ActionCode.CREATE);
            assertThat(version.getPermGroupCode()).isEqualTo("PERM_GROUP");
            assertThat(version.getOrgGroupCode()).isEqualTo("ORG_GROUP");
            assertThat(version.getDataKinds()).containsExactlyInAnyOrder(DataKind.SSN, DataKind.PHONE);
            assertThat(version.getMaskingEnabled()).isTrue();
            assertThat(version.getAuditEnabled()).isTrue();
            assertThat(version.getPriority()).isEqualTo(50);
            assertThat(version.isActive()).isTrue();
            assertThat(version.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(version.getChangeAction()).isEqualTo(ChangeAction.CREATE);
            assertThat(version.getChangeReason()).isEqualTo("생성");
            assertThat(version.getChangedBy()).isEqualTo("user");
            assertThat(version.getChangedByName()).isEqualTo("사용자");
            assertThat(version.getValidFrom()).isEqualTo(now);
            assertThat(version.getValidTo()).isNull();
        }

        @Test
        @DisplayName("Given: null 값들 / When: create 호출 / Then: 기본값 적용")
        void appliesDefaultValues() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());

            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책명", null,
                    FeatureCode.DRAFT, null, null, null,
                    null, null, null, null, true,
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.getDescription()).isNull();
            assertThat(version.getDataKinds()).isEmpty();
            assertThat(version.getMaskingEnabled()).isTrue();
            assertThat(version.getAuditEnabled()).isFalse();
            assertThat(version.getPriority()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("createDraft 팩토리 메서드")
    class CreateDraft {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: createDraft 호출 / Then: DRAFT 상태의 버전 생성")
        void createsDraftVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            MaskingPolicyVersion version = MaskingPolicyVersion.createDraft(
                    root, 2, "초안", "초안 설명",
                    FeatureCode.DRAFT, null, null, null,
                    Set.of(DataKind.EMAIL), true, false, 100, true,
                    null, null,
                    "초안 작성", "user", "사용자", now);

            assertThat(version.getVersion()).isEqualTo(2);
            assertThat(version.getName()).isEqualTo("초안");
            assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT);
            assertThat(version.getChangeAction()).isEqualTo(ChangeAction.DRAFT);
            assertThat(version.isDraft()).isTrue();
            assertThat(version.isCurrent()).isFalse();
        }
    }

    @Nested
    @DisplayName("createFromRollback 팩토리 메서드")
    class CreateFromRollback {

        @Test
        @DisplayName("Given: 롤백 파라미터 / When: createFromRollback 호출 / Then: ROLLBACK 액션의 버전 생성")
        void createsRollbackVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            OffsetDateTime now = OffsetDateTime.now();

            MaskingPolicyVersion version = MaskingPolicyVersion.createFromRollback(
                    root, 3, "롤백됨", "롤백 설명",
                    FeatureCode.DRAFT, null, null, null,
                    Set.of(DataKind.SSN), true, false, 100, true,
                    null, null,
                    "버전 1로 롤백", "user", "사용자", now, 1);

            assertThat(version.getVersion()).isEqualTo(3);
            assertThat(version.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(version.getChangeAction()).isEqualTo(ChangeAction.ROLLBACK);
            assertThat(version.getRollbackFromVersion()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("상태 확인 메서드")
    class StateChecks {

        @Test
        @DisplayName("Given: PUBLISHED 상태이고 validTo가 null / When: isCurrent 호출 / Then: true 반환")
        void isCurrentReturnsTrue() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createPublishedVersion(root, 1);

            assertThat(version.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("Given: DRAFT 상태 / When: isCurrent 호출 / Then: false 반환")
        void isCurrentReturnsFalseForDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createDraftVersion(root, 1);

            assertThat(version.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("Given: DRAFT 상태 / When: isDraft 호출 / Then: true 반환")
        void isDraftReturnsTrue() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createDraftVersion(root, 1);

            assertThat(version.isDraft()).isTrue();
        }

        @Test
        @DisplayName("Given: PUBLISHED 상태 / When: isDraft 호출 / Then: false 반환")
        void isDraftReturnsFalseForPublished() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createPublishedVersion(root, 1);

            assertThat(version.isDraft()).isFalse();
        }
    }

    @Nested
    @DisplayName("close 메서드")
    class Close {

        @Test
        @DisplayName("Given: PUBLISHED 버전 / When: close 호출 / Then: HISTORICAL 상태로 전환")
        void closesPublishedVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createPublishedVersion(root, 1);
            OffsetDateTime closeTime = OffsetDateTime.now();

            version.close(closeTime);

            assertThat(version.getStatus()).isEqualTo(VersionStatus.HISTORICAL);
            assertThat(version.getValidTo()).isEqualTo(closeTime);
        }

        @Test
        @DisplayName("Given: DRAFT 버전 / When: close 호출 / Then: validTo만 설정되고 상태 유지")
        void doesNotChangeStatusForDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createDraftVersion(root, 1);
            OffsetDateTime closeTime = OffsetDateTime.now();

            version.close(closeTime);

            assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT);
            assertThat(version.getValidTo()).isEqualTo(closeTime);
        }
    }

    @Nested
    @DisplayName("publish 메서드")
    class Publish {

        @Test
        @DisplayName("Given: DRAFT 버전 / When: publish 호출 / Then: PUBLISHED 상태로 전환")
        void publishesDraftVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createDraftVersion(root, 1);
            OffsetDateTime publishTime = OffsetDateTime.now();

            version.publish(publishTime);

            assertThat(version.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(version.getChangeAction()).isEqualTo(ChangeAction.PUBLISH);
            assertThat(version.getValidFrom()).isEqualTo(publishTime);
            assertThat(version.getChangedAt()).isEqualTo(publishTime);
        }

        @Test
        @DisplayName("Given: PUBLISHED 버전 / When: publish 호출 / Then: IllegalStateException 발생")
        void throwsExceptionForPublished() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createPublishedVersion(root, 1);

            assertThatThrownBy(() -> version.publish(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("초안 상태에서만 게시할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("updateDraft 메서드")
    class UpdateDraft {

        @Test
        @DisplayName("Given: DRAFT 버전 / When: updateDraft 호출 / Then: 필드 업데이트")
        void updatesDraftVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createDraftVersion(root, 1);
            OffsetDateTime updateTime = OffsetDateTime.now();

            version.updateDraft(
                    "수정된 이름", "수정된 설명",
                    FeatureCode.AUDIT_LOG, ActionCode.UPDATE,
                    "NEW_PERM", "NEW_ORG",
                    Set.of(DataKind.ACCOUNT_NO), false, true, 200, false,
                    null, null,
                    "수정 사유", updateTime);

            assertThat(version.getName()).isEqualTo("수정된 이름");
            assertThat(version.getDescription()).isEqualTo("수정된 설명");
            assertThat(version.getFeatureCode()).isEqualTo(FeatureCode.AUDIT_LOG);
            assertThat(version.getActionCode()).isEqualTo(ActionCode.UPDATE);
            assertThat(version.getPermGroupCode()).isEqualTo("NEW_PERM");
            assertThat(version.getOrgGroupCode()).isEqualTo("NEW_ORG");
            assertThat(version.getDataKinds()).containsExactly(DataKind.ACCOUNT_NO);
            assertThat(version.getMaskingEnabled()).isFalse();
            assertThat(version.getAuditEnabled()).isTrue();
            assertThat(version.getPriority()).isEqualTo(200);
            assertThat(version.isActive()).isFalse();
            assertThat(version.getChangeReason()).isEqualTo("수정 사유");
            assertThat(version.getChangedAt()).isEqualTo(updateTime);
        }

        @Test
        @DisplayName("Given: PUBLISHED 버전 / When: updateDraft 호출 / Then: IllegalStateException 발생")
        void throwsExceptionForPublished() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createPublishedVersion(root, 1);

            assertThatThrownBy(() -> version.updateDraft(
                    "새이름", null, FeatureCode.DRAFT, null, null, null,
                    null, null, null, null, true, null, null, null, OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("초안 상태에서만 수정할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("appliesTo 메서드")
    class AppliesTo {

        @Test
        @DisplayName("Given: dataKinds에 SSN 포함 / When: appliesTo(SSN) 호출 / Then: true 반환")
        void returnsTrueWhenKindExists() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    Set.of(DataKind.SSN, DataKind.PHONE), true, false, 100, true,
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.appliesTo(DataKind.SSN)).isTrue();
            assertThat(version.appliesTo(DataKind.PHONE)).isTrue();
        }

        @Test
        @DisplayName("Given: dataKinds가 비어있음 / When: appliesTo 호출 / Then: 모든 종류에 대해 true 반환")
        void returnsTrueForEmptyDataKinds() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    Set.of(), true, false, 100, true,
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.appliesTo(DataKind.SSN)).isTrue();
            assertThat(version.appliesTo(DataKind.EMAIL)).isTrue();
            assertThat(version.appliesTo(DataKind.ACCOUNT_NO)).isTrue();
        }

        @Test
        @DisplayName("Given: dataKinds에 EMAIL 없음 / When: appliesTo(EMAIL) 호출 / Then: false 반환")
        void returnsFalseWhenKindNotExists() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    Set.of(DataKind.SSN), true, false, 100, true,
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.appliesTo(DataKind.EMAIL)).isFalse();
        }
    }

    @Nested
    @DisplayName("isEffectiveAt 메서드")
    class IsEffectiveAt {

        @Test
        @DisplayName("Given: 유효 기간 내 / When: isEffectiveAt 호출 / Then: true 반환")
        void returnsTrueWithinEffectivePeriod() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            Instant effectiveFrom = Instant.now().minusSeconds(3600);
            Instant effectiveTo = Instant.now().plusSeconds(3600);
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    null, true, false, 100, true,
                    effectiveFrom, effectiveTo,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.isEffectiveAt(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("Given: effectiveFrom 이전 / When: isEffectiveAt 호출 / Then: false 반환")
        void returnsFalseBeforeEffectiveFrom() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            Instant effectiveFrom = Instant.now().plusSeconds(3600);
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    null, true, false, 100, true,
                    effectiveFrom, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Given: effectiveTo 이후 / When: isEffectiveAt 호출 / Then: false 반환")
        void returnsFalseAfterEffectiveTo() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            Instant effectiveTo = Instant.now().minusSeconds(3600);
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    null, true, false, 100, true,
                    null, effectiveTo,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("Given: active = false / When: isEffectiveAt 호출 / Then: false 반환")
        void returnsFalseWhenInactive() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    null, true, false, 100, false,  // active = false
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            assertThat(version.isEffectiveAt(Instant.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("matches 메서드")
    class Matches {

        @Test
        @DisplayName("Given: 모든 조건 일치 / When: matches 호출 / Then: true 반환")
        void returnsTrueWhenAllConditionsMatch() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, ActionCode.READ, "PERM_GROUP", null,
                    Set.of(DataKind.SSN), true, false, 100, true,
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            boolean result = version.matches(FeatureCode.DRAFT, ActionCode.READ, "PERM_GROUP", DataKind.SSN, Instant.now());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Given: featureCode 불일치 / When: matches 호출 / Then: false 반환")
        void returnsFalseWhenFeatureCodeMismatch() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, null, null, null,
                    null, true, false, 100, true,
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            boolean result = version.matches(FeatureCode.AUDIT_LOG, null, null, null, Instant.now());

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Given: actionCode 불일치 / When: matches 호출 / Then: false 반환")
        void returnsFalseWhenActionCodeMismatch() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = MaskingPolicyVersion.create(
                    root, 1, "정책", null,
                    FeatureCode.DRAFT, ActionCode.CREATE, null, null,
                    null, true, false, 100, true,
                    null, null,
                    ChangeAction.CREATE, null, "user", null, OffsetDateTime.now());

            boolean result = version.matches(FeatureCode.DRAFT, ActionCode.READ, null, null, Instant.now());

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("setVersionTag 메서드")
    class SetVersionTag {

        @Test
        @DisplayName("Given: 버전 / When: setVersionTag 호출 / Then: 태그 설정")
        void setsVersionTag() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createPublishedVersion(root, 1);

            version.setVersionTag("v1.0.0");

            assertThat(version.getVersionTag()).isEqualTo("v1.0.0");
        }
    }

    // === 헬퍼 메서드 ===

    private MaskingPolicyVersion createPublishedVersion(MaskingPolicyRoot root, int versionNumber) {
        return MaskingPolicyVersion.create(
                root, versionNumber, "정책", "설명",
                FeatureCode.DRAFT, null, null, null,
                Set.of(DataKind.SSN), true, false, 100, true,
                null, null,
                ChangeAction.CREATE, null, "user", "사용자",
                OffsetDateTime.now());
    }

    private MaskingPolicyVersion createDraftVersion(MaskingPolicyRoot root, int versionNumber) {
        return MaskingPolicyVersion.createDraft(
                root, versionNumber, "초안", "설명",
                FeatureCode.DRAFT, null, null, null,
                Set.of(DataKind.SSN), true, false, 100, true,
                null, null,
                null, "user", "사용자",
                OffsetDateTime.now());
    }
}
