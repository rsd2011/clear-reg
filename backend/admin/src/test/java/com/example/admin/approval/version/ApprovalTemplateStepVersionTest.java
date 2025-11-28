package com.example.admin.approval.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.common.version.ChangeAction;

@DisplayName("ApprovalTemplateStepVersion 엔티티")
class ApprovalTemplateStepVersionTest {

    private ApprovalLineTemplate createTestTemplate() {
        return ApprovalLineTemplate.create("테스트 템플릿", 1, "설명", OffsetDateTime.now());
    }

    private ApprovalLineTemplateVersion createTestVersion(ApprovalLineTemplate template) {
        return ApprovalLineTemplateVersion.create(
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
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = createTestVersion(template);
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");

            ApprovalTemplateStepVersion step = ApprovalTemplateStepVersion.create(version, 1, group);

            assertThat(step.getTemplateVersion()).isEqualTo(version);
            assertThat(step.getStepOrder()).isEqualTo(1);
            assertThat(step.getApprovalGroup()).isEqualTo(group);
            assertThat(step.getApprovalGroupCode()).isEqualTo("TEAM_LEADER");
            assertThat(step.getApprovalGroupName()).isEqualTo("팀장");
        }

        @Test
        @DisplayName("Given: 여러 순서의 Step / When: create 호출 / Then: 각각 순서대로 생성")
        void createsMultipleStepsInOrder() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = createTestVersion(template);
            ApprovalGroup group1 = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalGroup group2 = createTestGroup("DEPT_HEAD", "부서장");

            ApprovalTemplateStepVersion step1 = ApprovalTemplateStepVersion.create(version, 1, group1);
            ApprovalTemplateStepVersion step2 = ApprovalTemplateStepVersion.create(version, 2, group2);

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
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version1 = createTestVersion(template);
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalTemplateStepVersion sourceStep = ApprovalTemplateStepVersion.create(version1, 1, group);

            ApprovalLineTemplateVersion version2 = ApprovalLineTemplateVersion.create(
                    template, 2, "템플릿명", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );

            ApprovalTemplateStepVersion copiedStep = ApprovalTemplateStepVersion.copyFrom(version2, sourceStep);

            assertThat(copiedStep.getTemplateVersion()).isEqualTo(version2);
            assertThat(copiedStep.getStepOrder()).isEqualTo(sourceStep.getStepOrder());
            assertThat(copiedStep.getApprovalGroupCode()).isEqualTo(sourceStep.getApprovalGroupCode());
            assertThat(copiedStep.getApprovalGroupName()).isEqualTo(sourceStep.getApprovalGroupName());
        }

        @Test
        @DisplayName("Given: 비정규화된 필드 / When: copyFrom 호출 / Then: 비정규화 데이터 유지")
        void preservesDenormalizedFields() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version1 = createTestVersion(template);
            ApprovalGroup group = createTestGroup("EXEC", "임원");
            ApprovalTemplateStepVersion sourceStep = ApprovalTemplateStepVersion.create(version1, 3, group);

            ApprovalLineTemplateVersion version2 = ApprovalLineTemplateVersion.create(
                    template, 2, "템플릿명", 0, "설명", true,
                    ChangeAction.UPDATE, null, "user", "사용자", OffsetDateTime.now()
            );

            ApprovalTemplateStepVersion copiedStep = ApprovalTemplateStepVersion.copyFrom(version2, sourceStep);

            // 비정규화 필드가 유지되어야 함
            assertThat(copiedStep.getApprovalGroupCode()).isEqualTo("EXEC");
            assertThat(copiedStep.getApprovalGroupName()).isEqualTo("임원");
        }
    }

    @Nested
    @DisplayName("getApprovalGroupId")
    class GetApprovalGroupId {

        @Test
        @DisplayName("Given: Step 버전 / When: getApprovalGroupId 호출 / Then: 그룹 ID 반환")
        void returnsGroupId() {
            ApprovalLineTemplate template = createTestTemplate();
            ApprovalLineTemplateVersion version = createTestVersion(template);
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalTemplateStepVersion step = ApprovalTemplateStepVersion.create(version, 1, group);

            UUID groupId = step.getApprovalGroupId();

            assertThat(groupId).isEqualTo(group.getId());
        }
    }
}
