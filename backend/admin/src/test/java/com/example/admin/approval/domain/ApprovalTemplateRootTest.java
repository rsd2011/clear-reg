package com.example.admin.approval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("ApprovalTemplateRoot 엔티티")
class ApprovalTemplateRootTest {

    @Nested
    @DisplayName("create 팩토리 메서드")
    class Create {

        @Test
        @DisplayName("Given: 유효한 시간 / When: create 호출 / Then: 템플릿 루트 생성")
        void createsTemplateRoot() {
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);

            assertThat(root.getTemplateCode()).isNotNull();
            assertThat(root.getCreatedAt()).isEqualTo(now);
            assertThat(root.getUpdatedAt()).isEqualTo(now);
            assertThat(root.getCurrentVersion()).isNull();
        }

        @Test
        @DisplayName("Given: 템플릿 코드 / When: createWithCode 호출 / Then: 지정된 코드로 생성")
        void createsTemplateRootWithCode() {
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalTemplateRoot root = ApprovalTemplateRoot.createWithCode("CUSTOM_CODE", now);

            assertThat(root.getTemplateCode()).isEqualTo("CUSTOM_CODE");
        }
    }

    @Nested
    @DisplayName("편의 메서드")
    class ConvenienceMethods {

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: getName 호출 / Then: null 반환")
        void returnsNullWhenNoCurrentVersion() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());

            assertThat(root.getName()).isNull();
            assertThat(root.getDescription()).isNull();
            assertThat(root.getDisplayOrder()).isEqualTo(0);
            assertThat(root.isActive()).isFalse();
        }

        @Test
        @DisplayName("Given: 현재 버전이 있을 때 / When: getName 호출 / Then: 버전의 값 반환")
        void returnsCurrentVersionValues() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate version = ApprovalTemplate.create(
                    root, 1, "템플릿명", 10, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            root.activateNewVersion(version, OffsetDateTime.now());

            assertThat(root.getName()).isEqualTo("템플릿명");
            assertThat(root.getDescription()).isEqualTo("설명");
            assertThat(root.getDisplayOrder()).isEqualTo(10);
            assertThat(root.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("touch")
    class Touch {

        @Test
        @DisplayName("Given: 템플릿 / When: touch 호출 / Then: updatedAt 갱신")
        void updatesTimestamp() {
            OffsetDateTime createTime = OffsetDateTime.now();
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(createTime);
            OffsetDateTime touchTime = createTime.plusHours(1);

            root.touch(touchTime);

            assertThat(root.getUpdatedAt()).isEqualTo(touchTime);
        }
    }

    @Nested
    @DisplayName("버전 관리")
    class VersionManagement {

        private ApprovalTemplate createVersion(ApprovalTemplateRoot root, int versionNumber) {
            return ApprovalTemplate.create(
                    root, versionNumber, "이름 v" + versionNumber, versionNumber * 10, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
        }

        private ApprovalTemplate createDraft(ApprovalTemplateRoot root, int versionNumber) {
            return ApprovalTemplate.createDraft(
                    root, versionNumber, "초안", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );
        }

        @Test
        @DisplayName("Given: 새 버전 / When: activateNewVersion 호출 / Then: 현재 버전 설정")
        void activatesNewVersion() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate version = createVersion(root, 1);
            OffsetDateTime now = OffsetDateTime.now();

            root.activateNewVersion(version, now);

            assertThat(root.getCurrentVersion()).isEqualTo(version);
            assertThat(root.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Given: 기존 버전이 있을 때 / When: activateNewVersion 호출 / Then: 기존 버전이 이전 버전으로 이동")
        void movesCurrentToPrevious() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate v1 = createVersion(root, 1);
            root.activateNewVersion(v1, OffsetDateTime.now());

            ApprovalTemplate v2 = createVersion(root, 2);
            root.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(root.getCurrentVersion()).isEqualTo(v2);
            assertThat(root.getPreviousVersion()).isEqualTo(v1);
        }

        @Test
        @DisplayName("Given: nextVersion이 있을 때 / When: activateNewVersion 호출 / Then: nextVersion이 null로 설정")
        void clearsNextVersion() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate v1 = createVersion(root, 1);
            root.activateNewVersion(v1, OffsetDateTime.now());

            ApprovalTemplate draft = createDraft(root, 2);
            root.setDraftVersion(draft);
            assertThat(root.getNextVersion()).isNotNull();

            ApprovalTemplate v2 = createVersion(root, 2);
            root.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(root.getNextVersion()).isNull();
        }

        @Test
        @DisplayName("Given: 템플릿 / When: setDraftVersion 호출 / Then: nextVersion이 설정됨")
        void setsDraftVersion() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate draft = createDraft(root, 2);

            root.setDraftVersion(draft);

            assertThat(root.getNextVersion()).isEqualTo(draft);
        }

        @Test
        @DisplayName("Given: 초안이 있을 때 / When: publishDraft 호출 / Then: 초안이 현재 버전으로 전환")
        void publishesDraft() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate v1 = createVersion(root, 1);
            root.activateNewVersion(v1, OffsetDateTime.now());

            ApprovalTemplate draft = createDraft(root, 2);
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
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());

            assertThatThrownBy(() -> root.publishDraft(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("게시할 초안 버전이 없습니다");
        }

        @Test
        @DisplayName("Given: nextVersion이 DRAFT가 아닐 때 / When: publishDraft 호출 / Then: IllegalStateException 발생")
        void throwsExceptionWhenNotDraft() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate published = createVersion(root, 2);
            root.setDraftVersion(published);

            assertThatThrownBy(() -> root.publishDraft(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Given: 초안이 있을 때 / When: discardDraft 호출 / Then: nextVersion이 null로 설정")
        void discardsDraft() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate draft = createDraft(root, 2);
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
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate v1 = ApprovalTemplate.create(
                    root, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            ApprovalTemplate v2 = ApprovalTemplate.create(
                    root, 2, "이름", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );
            root.activateNewVersion(v1, OffsetDateTime.now());
            root.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(root.canRollback()).isTrue();
        }

        @Test
        @DisplayName("Given: previousVersion이 없을 때 / When: canRollback 호출 / Then: false 반환")
        void returnsFalseWhenNoPrevious() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());

            assertThat(root.canRollback()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCurrentVersionNumber")
    class GetCurrentVersionNumber {

        @Test
        @DisplayName("Given: 현재 버전이 있을 때 / When: getCurrentVersionNumber 호출 / Then: 버전 번호 반환")
        void returnsVersionNumber() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate version = ApprovalTemplate.create(
                    root, 5, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            root.activateNewVersion(version, OffsetDateTime.now());

            assertThat(root.getCurrentVersionNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: getCurrentVersionNumber 호출 / Then: null 반환")
        void returnsNullWhenNoVersion() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());

            assertThat(root.getCurrentVersionNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("hasDraft")
    class HasDraft {

        @Test
        @DisplayName("Given: DRAFT 상태의 nextVersion이 있을 때 / When: hasDraft 호출 / Then: true 반환")
        void returnsTrueWhenDraftExists() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate draft = ApprovalTemplate.createDraft(
                    root, 2, "초안", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );
            root.setDraftVersion(draft);

            assertThat(root.hasDraft()).isTrue();
        }

        @Test
        @DisplayName("Given: nextVersion이 없을 때 / When: hasDraft 호출 / Then: false 반환")
        void returnsFalseWhenNoDraft() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());

            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("Given: PUBLISHED 상태의 nextVersion이 있을 때 / When: hasDraft 호출 / Then: false 반환")
        void returnsFalseWhenNotDraft() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(OffsetDateTime.now());
            ApprovalTemplate published = ApprovalTemplate.create(
                    root, 2, "이름", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );
            root.setDraftVersion(published);

            assertThat(root.hasDraft()).isFalse();
        }
    }
}
