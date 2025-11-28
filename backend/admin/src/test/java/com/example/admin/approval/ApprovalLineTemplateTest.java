package com.example.admin.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.version.ApprovalLineTemplateVersion;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("ApprovalLineTemplate 엔티티")
class ApprovalLineTemplateTest {

    private ApprovalGroup createTestGroup(String code, String name) {
        return ApprovalGroup.create(code, name, "설명", 1, OffsetDateTime.now());
    }

    @Nested
    @DisplayName("create 팩토리 메서드")
    class Create {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: create 호출 / Then: 템플릿 생성")
        void createsTemplate() {
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalLineTemplate template = ApprovalLineTemplate.create("테스트 템플릿", 10, "설명", now);

            assertThat(template.getName()).isEqualTo("테스트 템플릿");
            assertThat(template.getDisplayOrder()).isEqualTo(10);
            assertThat(template.getDescription()).isEqualTo("설명");
            assertThat(template.isActive()).isTrue();
            assertThat(template.getCreatedAt()).isEqualTo(now);
            assertThat(template.getUpdatedAt()).isEqualTo(now);
            assertThat(template.getTemplateCode()).isNotNull();
        }

        @Test
        @DisplayName("Given: displayOrder가 null인 경우 / When: create 호출 / Then: 기본값 0으로 설정")
        void setsDefaultDisplayOrderWhenNull() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", null, "설명", OffsetDateTime.now());

            assertThat(template.getDisplayOrder()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("rename")
    class Rename {

        @Test
        @DisplayName("Given: 템플릿 / When: rename 호출 / Then: 필드 업데이트")
        void renamesTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("원본", 1, "원본 설명", OffsetDateTime.now());
            OffsetDateTime updateTime = OffsetDateTime.now().plusHours(1);

            template.rename("수정된 이름", 20, "수정된 설명", false, updateTime);

            assertThat(template.getName()).isEqualTo("수정된 이름");
            assertThat(template.getDisplayOrder()).isEqualTo(20);
            assertThat(template.getDescription()).isEqualTo("수정된 설명");
            assertThat(template.isActive()).isFalse();
            assertThat(template.getUpdatedAt()).isEqualTo(updateTime);
        }

        @Test
        @DisplayName("Given: displayOrder가 null인 경우 / When: rename 호출 / Then: 기존 값 유지")
        void keepsExistingDisplayOrderWhenNull() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("원본", 15, "설명", OffsetDateTime.now());

            template.rename("새 이름", null, "새 설명", true, OffsetDateTime.now());

            assertThat(template.getDisplayOrder()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Step 관리")
    class StepManagement {

        @Test
        @DisplayName("Given: 템플릿 / When: addStep 호출 / Then: Step이 추가되고 정렬됨")
        void addsStepInOrder() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalGroup group1 = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalGroup group2 = createTestGroup("DEPT_HEAD", "부서장");

            template.addStep(2, group2);
            template.addStep(1, group1);

            assertThat(template.getSteps()).hasSize(2);
            assertThat(template.getSteps().get(0).getStepOrder()).isEqualTo(1);
            assertThat(template.getSteps().get(1).getStepOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Given: 기존 Step이 있는 템플릿 / When: replaceSteps 호출 / Then: Step이 교체되고 정렬됨")
        void replacesStepsInOrder() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalGroup group1 = createTestGroup("OLD_GROUP", "구 그룹");
            template.addStep(1, group1);

            ApprovalGroup newGroup1 = createTestGroup("NEW_GROUP_1", "새 그룹1");
            ApprovalGroup newGroup2 = createTestGroup("NEW_GROUP_2", "새 그룹2");
            ApprovalTemplateStep newStep2 = new ApprovalTemplateStep(template, 2, newGroup2);
            ApprovalTemplateStep newStep1 = new ApprovalTemplateStep(template, 1, newGroup1);

            template.replaceSteps(List.of(newStep2, newStep1));

            assertThat(template.getSteps()).hasSize(2);
            assertThat(template.getSteps().get(0).getStepOrder()).isEqualTo(1);
            assertThat(template.getSteps().get(1).getStepOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("버전 관리")
    class VersionManagement {

        private ApprovalLineTemplateVersion createVersion(ApprovalLineTemplate template, int versionNumber) {
            return ApprovalLineTemplateVersion.create(
                    template, versionNumber, "이름 v" + versionNumber, versionNumber * 10, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
        }

        private ApprovalLineTemplateVersion createDraft(ApprovalLineTemplate template, int versionNumber) {
            return ApprovalLineTemplateVersion.createDraft(
                    template, versionNumber, "초안", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );
        }

        @Test
        @DisplayName("Given: 새 버전 / When: activateNewVersion 호출 / Then: 현재 버전 설정 및 필드 동기화")
        void activatesNewVersion() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion version = createVersion(template, 1);
            OffsetDateTime now = OffsetDateTime.now();

            template.activateNewVersion(version, now);

            assertThat(template.getCurrentVersion()).isEqualTo(version);
            assertThat(template.getName()).isEqualTo("이름 v1");
            assertThat(template.getDisplayOrder()).isEqualTo(10);
            assertThat(template.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Given: 기존 버전이 있을 때 / When: activateNewVersion 호출 / Then: 기존 버전이 이전 버전으로 이동")
        void movesCurrentToPrevious() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion v1 = createVersion(template, 1);
            template.activateNewVersion(v1, OffsetDateTime.now());

            ApprovalLineTemplateVersion v2 = createVersion(template, 2);
            template.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(template.getCurrentVersion()).isEqualTo(v2);
            assertThat(template.getPreviousVersion()).isEqualTo(v1);
        }

        @Test
        @DisplayName("Given: nextVersion이 있을 때 / When: activateNewVersion 호출 / Then: nextVersion이 null로 설정")
        void clearsNextVersion() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion v1 = createVersion(template, 1);
            template.activateNewVersion(v1, OffsetDateTime.now());

            ApprovalLineTemplateVersion draft = createDraft(template, 2);
            template.setDraftVersion(draft);
            assertThat(template.getNextVersion()).isNotNull();

            ApprovalLineTemplateVersion v2 = createVersion(template, 2);
            template.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(template.getNextVersion()).isNull();
        }

        @Test
        @DisplayName("Given: 템플릿 / When: setDraftVersion 호출 / Then: nextVersion이 설정됨")
        void setsDraftVersion() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion draft = createDraft(template, 2);

            template.setDraftVersion(draft);

            assertThat(template.getNextVersion()).isEqualTo(draft);
        }

        @Test
        @DisplayName("Given: 초안이 있을 때 / When: publishDraft 호출 / Then: 초안이 현재 버전으로 전환")
        void publishesDraft() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion v1 = createVersion(template, 1);
            template.activateNewVersion(v1, OffsetDateTime.now());

            ApprovalLineTemplateVersion draft = createDraft(template, 2);
            template.setDraftVersion(draft);
            OffsetDateTime publishTime = OffsetDateTime.now();

            template.publishDraft(publishTime);

            assertThat(template.getCurrentVersion()).isEqualTo(draft);
            assertThat(template.getPreviousVersion()).isEqualTo(v1);
            assertThat(template.getNextVersion()).isNull();
            assertThat(draft.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Given: 초안이 없을 때 / When: publishDraft 호출 / Then: IllegalStateException 발생")
        void throwsExceptionWhenNoDraft() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());

            assertThatThrownBy(() -> template.publishDraft(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("게시할 초안 버전이 없습니다");
        }

        @Test
        @DisplayName("Given: nextVersion이 DRAFT가 아닐 때 / When: publishDraft 호출 / Then: IllegalStateException 발생")
        void throwsExceptionWhenNotDraft() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion published = createVersion(template, 2);
            // 강제로 nextVersion에 PUBLISHED 버전 설정 (비정상 상황)
            template.setDraftVersion(published);

            assertThatThrownBy(() -> template.publishDraft(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Given: 초안이 있을 때 / When: discardDraft 호출 / Then: nextVersion이 null로 설정")
        void discardsDraft() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion draft = createDraft(template, 2);
            template.setDraftVersion(draft);

            template.discardDraft();

            assertThat(template.getNextVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("canRollback")
    class CanRollback {

        @Test
        @DisplayName("Given: previousVersion이 있을 때 / When: canRollback 호출 / Then: true 반환")
        void returnsTrueWhenPreviousExists() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion v1 = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            ApprovalLineTemplateVersion v2 = ApprovalLineTemplateVersion.create(
                    template, 2, "이름", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );
            template.activateNewVersion(v1, OffsetDateTime.now());
            template.activateNewVersion(v2, OffsetDateTime.now());

            assertThat(template.canRollback()).isTrue();
        }

        @Test
        @DisplayName("Given: previousVersion이 없을 때 / When: canRollback 호출 / Then: false 반환")
        void returnsFalseWhenNoPrevious() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());

            assertThat(template.canRollback()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCurrentVersionNumber")
    class GetCurrentVersionNumber {

        @Test
        @DisplayName("Given: 현재 버전이 있을 때 / When: getCurrentVersionNumber 호출 / Then: 버전 번호 반환")
        void returnsVersionNumber() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 5, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            template.activateNewVersion(version, OffsetDateTime.now());

            assertThat(template.getCurrentVersionNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("Given: 현재 버전이 없을 때 / When: getCurrentVersionNumber 호출 / Then: null 반환")
        void returnsNullWhenNoVersion() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());

            assertThat(template.getCurrentVersionNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("hasDraft")
    class HasDraft {

        @Test
        @DisplayName("Given: DRAFT 상태의 nextVersion이 있을 때 / When: hasDraft 호출 / Then: true 반환")
        void returnsTrueWhenDraftExists() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "초안", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );
            template.setDraftVersion(draft);

            assertThat(template.hasDraft()).isTrue();
        }

        @Test
        @DisplayName("Given: nextVersion이 없을 때 / When: hasDraft 호출 / Then: false 반환")
        void returnsFalseWhenNoDraft() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());

            assertThat(template.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("Given: PUBLISHED 상태의 nextVersion이 있을 때 / When: hasDraft 호출 / Then: false 반환")
        void returnsFalseWhenNotDraft() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 1, "설명", OffsetDateTime.now());
            ApprovalLineTemplateVersion published = ApprovalLineTemplateVersion.create(
                    template, 2, "이름", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );
            template.setDraftVersion(published);

            assertThat(template.hasDraft()).isFalse();
        }
    }
}
