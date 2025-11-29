package com.example.admin.approval.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.version.ChangeAction;

@DisplayName("ApprovalTemplateStep 엔티티")
class ApprovalTemplateStepTest {

    private ApprovalTemplateRoot createTestRoot() {
        return ApprovalTemplateRoot.create(OffsetDateTime.now());
    }

    private ApprovalTemplate createTestVersion(ApprovalTemplateRoot template) {
        return ApprovalTemplate.create(
                template, 1, "템플릿명", 0, "설명", true,
                ChangeAction.CREATE, null, "user", "사용자", OffsetDateTime.now()
        );
    }

    private ApprovalGroup createTestGroup(String code, String name) {
        return ApprovalGroup.create(code, name, "설명", 1, OffsetDateTime.now());
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: create 호출 / Then: Step 버전 생성")
        void createsStepVersionWithValidParams() {
            ApprovalTemplateRoot template = createTestRoot();
            ApprovalTemplate version = createTestVersion(template);
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");

            ApprovalTemplateStep step = ApprovalTemplateStep.create(version, 1, group, false);

            assertThat(step.getTemplate()).isEqualTo(version);
            assertThat(step.getStepOrder()).isEqualTo(1);
            assertThat(step.getApprovalGroup()).isEqualTo(group);
            assertThat(step.getApprovalGroupCode()).isEqualTo("TEAM_LEADER");
            assertThat(step.getApprovalGroupName()).isEqualTo("팀장");
            assertThat(step.isSkippable()).isFalse();
        }

        @Test
        @DisplayName("Given: 여러 순서의 Step / When: create 호출 / Then: 각각 순서대로 생성")
        void createsMultipleStepsInOrder() {
            ApprovalTemplateRoot template = createTestRoot();
            ApprovalTemplate version = createTestVersion(template);
            ApprovalGroup group1 = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalGroup group2 = createTestGroup("DEPT_HEAD", "부서장");

            ApprovalTemplateStep step1 = ApprovalTemplateStep.create(version, 1, group1, false);
            ApprovalTemplateStep step2 = ApprovalTemplateStep.create(version, 2, group2, true);

            assertThat(step1.getStepOrder()).isEqualTo(1);
            assertThat(step2.getStepOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("copyFrom")
    class CopyFrom {

        @Test
        @DisplayName("Given: 원본 Step 버전 / When: copyFrom 호출 / Then: 새 버전에 복사")
        void copiesStepToNewVersion() {
            ApprovalTemplateRoot template = createTestRoot();
            ApprovalTemplate version1 = createTestVersion(template);
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalTemplateStep sourceStep = ApprovalTemplateStep.create(version1, 1, group, true);

            ApprovalTemplate version2 = ApprovalTemplate.create(
                    template, 2, "템플릿명", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );

            ApprovalTemplateStep copiedStep = ApprovalTemplateStep.copyFrom(version2, sourceStep);

            assertThat(copiedStep.getTemplate()).isEqualTo(version2);
            assertThat(copiedStep.getStepOrder()).isEqualTo(sourceStep.getStepOrder());
            assertThat(copiedStep.getApprovalGroupCode()).isEqualTo(sourceStep.getApprovalGroupCode());
            assertThat(copiedStep.getApprovalGroupName()).isEqualTo(sourceStep.getApprovalGroupName());
            assertThat(copiedStep.isSkippable()).isEqualTo(sourceStep.isSkippable());
        }

        @Test
        @DisplayName("Given: 비정규화된 필드 / When: copyFrom 호출 / Then: 비정규화 데이터 유지")
        void preservesDenormalizedFields() {
            ApprovalTemplateRoot template = createTestRoot();
            ApprovalTemplate version1 = createTestVersion(template);
            ApprovalGroup group = createTestGroup("EXEC", "임원");
            ApprovalTemplateStep sourceStep = ApprovalTemplateStep.create(version1, 3, group, false);

            ApprovalTemplate version2 = ApprovalTemplate.create(
                    template, 2, "템플릿명", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );

            ApprovalTemplateStep copiedStep = ApprovalTemplateStep.copyFrom(version2, sourceStep);

            // 비정규화 필드가 유지되어야 함
            assertThat(copiedStep.getApprovalGroupCode()).isEqualTo("EXEC");
            assertThat(copiedStep.getApprovalGroupName()).isEqualTo("임원");
            assertThat(copiedStep.isSkippable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getApprovalGroupId")
    class GetApprovalGroupId {

        @Test
        @DisplayName("Given: Step 버전 / When: getApprovalGroupId 호출 / Then: 그룹 ID 반환")
        void returnsGroupId() {
            ApprovalTemplateRoot template = createTestRoot();
            ApprovalTemplate version = createTestVersion(template);
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalTemplateStep step = ApprovalTemplateStep.create(version, 1, group, false);

            UUID groupId = step.getApprovalGroupId();

            assertThat(groupId).isEqualTo(group.getId());
        }
    }
}
