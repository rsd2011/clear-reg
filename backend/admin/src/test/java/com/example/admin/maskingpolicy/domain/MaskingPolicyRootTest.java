package com.example.admin.maskingpolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.domain.FeatureCode;
import com.example.common.masking.DataKind;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("MaskingPolicyRoot 엔티티")
class MaskingPolicyRootTest {

    @Nested
    @DisplayName("create 팩토리 메서드")
    class Create {

        @Test
        @DisplayName("Given: 유효한 시간 / When: create 호출 / Then: 정책 루트 생성")
        void createsPolicyRoot() {
            OffsetDateTime now = OffsetDateTime.now();

            MaskingPolicyRoot root = MaskingPolicyRoot.create(now);

            assertThat(root.getPolicyCode()).isNotNull();
            assertThat(root.getCreatedAt()).isEqualTo(now);
            assertThat(root.getUpdatedAt()).isEqualTo(now);
            assertThat(root.getCurrentVersion()).isNull();
        }

        @Test
        @DisplayName("Given: 정책 코드 / When: createWithCode 호출 / Then: 지정된 코드로 생성")
        void createsPolicyRootWithCode() {
            OffsetDateTime now = OffsetDateTime.now();

            MaskingPolicyRoot root = MaskingPolicyRoot.createWithCode("CUSTOM_POLICY_CODE", now);

            assertThat(root.getPolicyCode()).isEqualTo("CUSTOM_POLICY_CODE");
        }
    }

    @Nested
    @DisplayName("편의 메서드")
    class ConvenienceMethods {

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: getName 호출 / Then: null 반환")
        void returnsNullWhenNoCurrentVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());

            assertThat(root.getName()).isNull();
            assertThat(root.getDescription()).isNull();
            assertThat(root.getPriority()).isNull();
            assertThat(root.isMaskingEnabled()).isNull();
            assertThat(root.isActive()).isFalse();
        }

        @Test
        @DisplayName("Given: 현재 버전이 있을 때 / When: getName 호출 / Then: 버전의 값 반환")
        void returnsCurrentVersionValues() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createVersion(root, 1, "정책명", true);
            root.activateNewVersion(version, OffsetDateTime.now());

            assertThat(root.getName()).isEqualTo("정책명");
            assertThat(root.getDescription()).isEqualTo("설명");
            assertThat(root.getPriority()).isEqualTo(100);
            assertThat(root.isMaskingEnabled()).isTrue();
            assertThat(root.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("touch")
    class Touch {

        @Test
        @DisplayName("Given: 정책 / When: touch 호출 / Then: updatedAt 갱신")
        void updatesTimestamp() {
            OffsetDateTime createTime = OffsetDateTime.now();
            MaskingPolicyRoot root = MaskingPolicyRoot.create(createTime);
            OffsetDateTime touchTime = createTime.plusHours(1);

            root.touch(touchTime);

            assertThat(root.getUpdatedAt()).isEqualTo(touchTime);
        }
    }

    @Nested
    @DisplayName("버전 관리")
    class VersionManagement {

        @Test
        @DisplayName("Given: 새 버전 / When: activateNewVersion 호출 / Then: 현재 버전 설정")
        void activatesNewVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createVersion(root, 1, "정책", true);
            OffsetDateTime now = OffsetDateTime.now();

            root.activateNewVersion(version, now);

            assertThat(root.getCurrentVersion()).isEqualTo(version);
            assertThat(root.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Given: 기존 버전이 있을 때 / When: activateNewVersion 호출 / Then: 기존 버전이 이전 버전으로 이동")
        void movesCurrentToPrevious() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion v1 = createVersion(root, 1, "v1", true);
            root.activateNewVersion(v1, OffsetDateTime.now());

            MaskingPolicyVersion v2 = createVersion(root, 2, "v2", true);
            root.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(root.getCurrentVersion()).isEqualTo(v2);
            assertThat(root.getPreviousVersion()).isEqualTo(v1);
        }

        @Test
        @DisplayName("Given: nextVersion이 있을 때 / When: activateNewVersion 호출 / Then: nextVersion이 null로 설정")
        void clearsNextVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion v1 = createVersion(root, 1, "v1", true);
            root.activateNewVersion(v1, OffsetDateTime.now());

            MaskingPolicyVersion draft = createDraft(root, 2);
            root.setDraftVersion(draft);
            assertThat(root.getNextVersion()).isNotNull();

            MaskingPolicyVersion v2 = createVersion(root, 2, "v2", true);
            root.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(root.getNextVersion()).isNull();
        }

        @Test
        @DisplayName("Given: 정책 / When: setDraftVersion 호출 / Then: nextVersion이 설정됨")
        void setsDraftVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion draft = createDraft(root, 1);

            root.setDraftVersion(draft);

            assertThat(root.getNextVersion()).isEqualTo(draft);
        }

        @Test
        @DisplayName("Given: 초안이 있을 때 / When: publishDraft 호출 / Then: 초안이 현재 버전으로 전환")
        void publishesDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion v1 = createVersion(root, 1, "v1", true);
            root.activateNewVersion(v1, OffsetDateTime.now());

            MaskingPolicyVersion draft = createDraft(root, 2);
            root.setDraftVersion(draft);
            OffsetDateTime publishTime = OffsetDateTime.now();

            root.publishDraft(publishTime);

            assertThat(root.getCurrentVersion()).isEqualTo(draft);
            assertThat(root.getPreviousVersion()).isEqualTo(v1);
            assertThat(root.getNextVersion()).isNull();
            assertThat(draft.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: publishDraft 호출 / Then: IllegalStateException 발생")
        void throwsExceptionWhenNoDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());

            assertThatThrownBy(() -> root.publishDraft(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("게시할 초안 버전이 없습니다");
        }

        @Test
        @DisplayName("Given: nextVersion이 DRAFT가 아닐 때 / When: publishDraft 호출 / Then: IllegalStateException 발생")
        void throwsExceptionWhenNotDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion published = createVersion(root, 1, "v1", true);
            root.setDraftVersion(published);

            assertThatThrownBy(() -> root.publishDraft(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Given: 초안이 있을 때 / When: discardDraft 호출 / Then: nextVersion이 null로 설정")
        void discardsDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion draft = createDraft(root, 1);
            root.setDraftVersion(draft);

            root.discardDraft();

            assertThat(root.getNextVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("canRollback")
    class CanRollback {

        @Test
        @DisplayName("Given: previousVersion이 있을 때 / When: canRollback 호출 / Then: true 반환")
        void returnsTrueWhenPreviousExists() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion v1 = createVersion(root, 1, "v1", true);
            MaskingPolicyVersion v2 = createVersion(root, 2, "v2", true);
            root.activateNewVersion(v1, OffsetDateTime.now());
            root.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(root.canRollback()).isTrue();
        }

        @Test
        @DisplayName("Given: previousVersion이 없을 때 / When: canRollback 호출 / Then: false 반환")
        void returnsFalseWhenNoPrevious() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());

            assertThat(root.canRollback()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCurrentVersionNumber")
    class GetCurrentVersionNumber {

        @Test
        @DisplayName("Given: 현재 버전이 있을 때 / When: getCurrentVersionNumber 호출 / Then: 버전 번호 반환")
        void returnsVersionNumber() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion version = createVersion(root, 5, "정책", true);
            root.activateNewVersion(version, OffsetDateTime.now());

            assertThat(root.getCurrentVersionNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: getCurrentVersionNumber 호출 / Then: null 반환")
        void returnsNullWhenNoVersion() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());

            assertThat(root.getCurrentVersionNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("hasDraft")
    class HasDraft {

        @Test
        @DisplayName("Given: DRAFT 상태의 nextVersion이 있을 때 / When: hasDraft 호출 / Then: true 반환")
        void returnsTrueWhenDraftExists() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion draft = createDraft(root, 1);
            root.setDraftVersion(draft);

            assertThat(root.hasDraft()).isTrue();
        }

        @Test
        @DisplayName("Given: nextVersion이 없을 때 / When: hasDraft 호출 / Then: false 반환")
        void returnsFalseWhenNoDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());

            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("Given: PUBLISHED 상태의 nextVersion이 있을 때 / When: hasDraft 호출 / Then: false 반환")
        void returnsFalseWhenNotDraft() {
            MaskingPolicyRoot root = MaskingPolicyRoot.create(OffsetDateTime.now());
            MaskingPolicyVersion published = createVersion(root, 1, "정책", true);
            root.setDraftVersion(published);

            assertThat(root.hasDraft()).isFalse();
        }
    }

    // === 헬퍼 메서드 ===

    private MaskingPolicyVersion createVersion(MaskingPolicyRoot root, int versionNumber, String name, boolean active) {
        return MaskingPolicyVersion.create(
                root, versionNumber, name, "설명",
                FeatureCode.DRAFT, null, null, null,
                Set.of(DataKind.SSN), true, false, 100, active,
                null, null,
                ChangeAction.CREATE, null, "user", "사용자",
                OffsetDateTime.now());
    }

    private MaskingPolicyVersion createDraft(MaskingPolicyRoot root, int versionNumber) {
        return MaskingPolicyVersion.createDraft(
                root, versionNumber, "초안", "설명",
                FeatureCode.DRAFT, null, null, null,
                Set.of(DataKind.SSN), true, false, 100, true,
                null, null,
                null, "user", "사용자",
                OffsetDateTime.now());
    }
}
