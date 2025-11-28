package com.example.admin.approval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("ApprovalLineTemplateVersion 엔티티")
class ApprovalLineTemplateVersionTest {

    private ApprovalLineTemplate createTestTemplate() {
        return ApprovalLineTemplate.create("테스트 템플릿", 1, "설명", OffsetDateTime.now());
    }

    private ApprovalGroup createTestGroup(String code, String name) {
        return ApprovalGroup.create(code, name, "설명", 1, OffsetDateTime.now());
    }

    @Nested
    @DisplayName("create 팩토리 메서드")
    class Create {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: create 호출 / Then: PUBLISHED 상태의 버전 생성")
        void createsPublishedVersion() {
            ApprovalLineTemplate template = createTestTemplate();
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 10, "설명", true,
                    ChangeAction.CREATE, "생성 사유", "user1", "사용자1", now
            );

            assertThat(version.getTemplate()).isEqualTo(template);
            assertThat(version.getVersion()).isEqualTo(1);
            assertThat(version.getName()).isEqualTo("이름");
            assertThat(version.getDisplayOrder()).isEqualTo(10);
            assertThat(version.getDescription()).isEqualTo("설명");
            assertThat(version.isActive()).isTrue();
            assertThat(version.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(version.getChangeAction()).isEqualTo(ChangeAction.CREATE);
            assertThat(version.getChangeReason()).isEqualTo("생성 사유");
            assertThat(version.getChangedBy()).isEqualTo("user1");
            assertThat(version.getChangedByName()).isEqualTo("사용자1");
            assertThat(version.getValidFrom()).isEqualTo(now);
            assertThat(version.getValidTo()).isNull();
        }

        @Test
        @DisplayName("Given: displayOrder가 null인 경우 / When: create 호출 / Then: 기본값 0으로 설정")
        void setsDefaultDisplayOrderWhenNull() {
            ApprovalLineTemplate template = createTestTemplate();
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", null, "설명", true,
                    ChangeAction.CREATE, null, "user1", "사용자1", now
            );

            assertThat(version.getDisplayOrder()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("createDraft 팩토리 메서드")
    class CreateDraft {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: createDraft 호출 / Then: DRAFT 상태의 버전 생성")
        void createsDraftVersion() {
            ApprovalLineTemplate template = createTestTemplate();
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "초안 이름", 5, "초안 설명", true,
                    "변경 사유", "user2", "사용자2", now
            );

            assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);
            assertThat(draft.getChangeAction()).isEqualTo(ChangeAction.DRAFT);
            assertThat(draft.getName()).isEqualTo("초안 이름");
        }
    }

    @Nested
    @DisplayName("createFromCopy 팩토리 메서드")
    class CreateFromCopy {

        @Test
        @DisplayName("Given: 원본 정보 / When: createFromCopy 호출 / Then: COPY 액션의 버전 생성")
        void createsCopyVersion() {
            ApprovalLineTemplate template = createTestTemplate();
            UUID sourceTemplateId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.createFromCopy(
                    template, 1, "복사본", 10, "복사 설명", true,
                    "user3", "사용자3", now, sourceTemplateId
            );

            assertThat(version.getChangeAction()).isEqualTo(ChangeAction.COPY);
            assertThat(version.getSourceTemplateId()).isEqualTo(sourceTemplateId);
            assertThat(version.getChangeReason()).isNull();
        }
    }

    @Nested
    @DisplayName("createFromRollback 팩토리 메서드")
    class CreateFromRollback {

        @Test
        @DisplayName("Given: 롤백 대상 버전 / When: createFromRollback 호출 / Then: ROLLBACK 액션의 버전 생성")
        void createsRollbackVersion() {
            ApprovalLineTemplate template = createTestTemplate();
            OffsetDateTime now = OffsetDateTime.now();

            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.createFromRollback(
                    template, 3, "롤백된 이름", 5, "설명", true,
                    "롤백 사유", "user4", "사용자4", now, 1
            );

            assertThat(version.getChangeAction()).isEqualTo(ChangeAction.ROLLBACK);
            assertThat(version.getRollbackFromVersion()).isEqualTo(1);
            assertThat(version.getChangeReason()).isEqualTo("롤백 사유");
        }
    }

    @Nested
    @DisplayName("isCurrent")
    class IsCurrent {

        @Test
        @DisplayName("Given: validTo가 null이고 PUBLISHED 상태 / When: isCurrent 호출 / Then: true 반환")
        void returnsTrueForCurrentVersion() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );

            assertThat(version.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("Given: validTo가 설정됨 / When: isCurrent 호출 / Then: false 반환")
        void returnsFalseWhenValidToIsSet() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            version.close(OffsetDateTime.now());

            assertThat(version.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("Given: DRAFT 상태 / When: isCurrent 호출 / Then: false 반환")
        void returnsFalseForDraftStatus() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "이름", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );

            assertThat(draft.isCurrent()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDraft")
    class IsDraft {

        @Test
        @DisplayName("Given: DRAFT 상태 / When: isDraft 호출 / Then: true 반환")
        void returnsTrueForDraftStatus() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "이름", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );

            assertThat(draft.isDraft()).isTrue();
        }

        @Test
        @DisplayName("Given: PUBLISHED 상태 / When: isDraft 호출 / Then: false 반환")
        void returnsFalseForPublishedStatus() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );

            assertThat(version.isDraft()).isFalse();
        }
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("Given: PUBLISHED 상태 / When: close 호출 / Then: HISTORICAL로 변경 및 validTo 설정")
        void closesPublishedVersion() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            OffsetDateTime closeTime = OffsetDateTime.now();

            version.close(closeTime);

            assertThat(version.getStatus()).isEqualTo(VersionStatus.HISTORICAL);
            assertThat(version.getValidTo()).isEqualTo(closeTime);
        }

        @Test
        @DisplayName("Given: DRAFT 상태 / When: close 호출 / Then: DRAFT 상태 유지 및 validTo만 설정")
        void doesNotChangeStatusForDraft() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "이름", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );
            OffsetDateTime closeTime = OffsetDateTime.now();

            draft.close(closeTime);

            assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);
            assertThat(draft.getValidTo()).isEqualTo(closeTime);
        }
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("Given: DRAFT 상태 / When: publish 호출 / Then: PUBLISHED로 변경")
        void publishesDraft() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "이름", 0, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );
            OffsetDateTime publishTime = OffsetDateTime.now();

            draft.publish(publishTime);

            assertThat(draft.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(draft.getChangeAction()).isEqualTo(ChangeAction.PUBLISH);
            assertThat(draft.getValidFrom()).isEqualTo(publishTime);
            assertThat(draft.getChangedAt()).isEqualTo(publishTime);
        }

        @Test
        @DisplayName("Given: PUBLISHED 상태 / When: publish 호출 / Then: IllegalStateException 발생")
        void throwsExceptionForNonDraft() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );

            assertThatThrownBy(() -> version.publish(OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("초안 상태에서만 게시");
        }
    }

    @Nested
    @DisplayName("updateDraft")
    class UpdateDraft {

        @Test
        @DisplayName("Given: DRAFT 상태 / When: updateDraft 호출 / Then: 필드 업데이트")
        void updatesDraftFields() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "원본 이름", 5, "원본 설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );
            OffsetDateTime updateTime = OffsetDateTime.now();

            draft.updateDraft("수정된 이름", 10, "수정된 설명", false, "변경 사유", updateTime);

            assertThat(draft.getName()).isEqualTo("수정된 이름");
            assertThat(draft.getDisplayOrder()).isEqualTo(10);
            assertThat(draft.getDescription()).isEqualTo("수정된 설명");
            assertThat(draft.isActive()).isFalse();
            assertThat(draft.getChangeReason()).isEqualTo("변경 사유");
            assertThat(draft.getChangedAt()).isEqualTo(updateTime);
        }

        @Test
        @DisplayName("Given: displayOrder가 null / When: updateDraft 호출 / Then: 기존 값 유지")
        void keepsExistingDisplayOrderWhenNull() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion draft = ApprovalLineTemplateVersion.createDraft(
                    template, 2, "이름", 15, "설명", true,
                    null, "user", "사용자", OffsetDateTime.now()
            );

            draft.updateDraft("새 이름", null, "새 설명", true, "사유", OffsetDateTime.now());

            assertThat(draft.getDisplayOrder()).isEqualTo(15);
        }

        @Test
        @DisplayName("Given: PUBLISHED 상태 / When: updateDraft 호출 / Then: IllegalStateException 발생")
        void throwsExceptionForNonDraft() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );

            assertThatThrownBy(() -> version.updateDraft("새 이름", 10, "새 설명", false, "사유", OffsetDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("초안 상태에서만 수정");
        }
    }

    @Nested
    @DisplayName("Step 관리")
    class StepManagement {

        @Test
        @DisplayName("Given: 버전 / When: addStep 호출 / Then: Step이 추가됨")
        void addsStep() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalTemplateStepVersion step = ApprovalTemplateStepVersion.create(version, 1, group);

            version.addStep(step);

            assertThat(version.getSteps()).hasSize(1);
            assertThat(version.getSteps().get(0)).isEqualTo(step);
        }

        @Test
        @DisplayName("Given: 기존 Step이 있는 버전 / When: replaceSteps 호출 / Then: Step이 교체됨")
        void replacesSteps() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );
            ApprovalGroup group1 = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalGroup group2 = createTestGroup("DEPT_HEAD", "부서장");
            ApprovalTemplateStepVersion oldStep = ApprovalTemplateStepVersion.create(version, 1, group1);
            version.addStep(oldStep);

            ApprovalTemplateStepVersion newStep = ApprovalTemplateStepVersion.create(version, 1, group2);
            version.replaceSteps(List.of(newStep));

            assertThat(version.getSteps()).hasSize(1);
            assertThat(version.getSteps().get(0).getApprovalGroupCode()).isEqualTo("DEPT_HEAD");
        }
    }

    @Nested
    @DisplayName("setVersionTag")
    class SetVersionTag {

        @Test
        @DisplayName("Given: 버전 / When: setVersionTag 호출 / Then: 태그 설정됨")
        void setsVersionTag() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = ApprovalLineTemplateVersion.create(
                    template, 1, "이름", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
            );

            version.setVersionTag("v1.0.0");

            assertThat(version.getVersionTag()).isEqualTo("v1.0.0");
        }
    }
}
