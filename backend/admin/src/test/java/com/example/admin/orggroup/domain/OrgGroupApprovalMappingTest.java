package com.example.admin.orggroup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.common.orggroup.WorkType;

@DisplayName("OrgGroupApprovalMapping 엔티티")
class OrgGroupApprovalMappingTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    private OrgGroup createOrgGroup() {
        return OrgGroup.builder()
                .code("SALES")
                .name("영업팀")
                .build();
    }

    private ApprovalTemplateRoot createTemplateRoot() {
        return ApprovalTemplateRoot.create(NOW);
    }

    private DraftFormTemplateRoot createDraftFormTemplateRoot() {
        return DraftFormTemplateRoot.create(NOW);
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("Given: 조직그룹과 업무유형, 템플릿이 주어지면 When: 매핑을 생성하면 Then: 정상적으로 생성된다")
        void createsWithWorkType() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();

            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.FILE_EXPORT, template, NOW);

            assertThat(mapping.getOrgGroup()).isEqualTo(orgGroup);
            assertThat(mapping.getWorkType()).isEqualTo(WorkType.FILE_EXPORT);
            assertThat(mapping.getApprovalTemplateRoot()).isEqualTo(template);
            assertThat(mapping.getCreatedAt()).isEqualTo(NOW);
            assertThat(mapping.getUpdatedAt()).isEqualTo(NOW);
            assertThat(mapping.isDefault()).isFalse();
        }

        @Test
        @DisplayName("Given: 업무유형이 null이면 When: 매핑을 생성하면 Then: 기본 템플릿으로 생성된다")
        void createsDefaultMapping() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();

            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, null, template, NOW);

            assertThat(mapping.getWorkType()).isNull();
            assertThat(mapping.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Given: createDefault 팩토리 메서드를 사용하면 When: 매핑을 생성하면 Then: 기본 템플릿으로 생성된다")
        void createsDefaultMappingViaFactory() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();

            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.createDefault(
                    orgGroup, template, NOW);

            assertThat(mapping.getWorkType()).isNull();
            assertThat(mapping.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Given: 조직그룹이 null이면 When: 매핑을 생성하면 Then: 예외가 발생한다")
        void throwsWhenOrgGroupNull() {
            ApprovalTemplateRoot template = createTemplateRoot();

            assertThatThrownBy(() ->
                    OrgGroupApprovalMapping.create(null, WorkType.GENERAL, template, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("조직그룹");
        }

        @Test
        @DisplayName("Given: 템플릿이 null이면 When: 매핑을 생성하면 Then: 예외가 발생한다")
        void throwsWhenTemplateNull() {
            OrgGroup orgGroup = createOrgGroup();

            assertThatThrownBy(() ->
                    OrgGroupApprovalMapping.create(orgGroup, WorkType.GENERAL, null, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("승인선 템플릿");
        }
    }

    @Nested
    @DisplayName("템플릿 변경")
    class ChangeTemplate {

        @Test
        @DisplayName("Given: 매핑이 있으면 When: 템플릿을 변경하면 Then: 템플릿과 수정일시가 변경된다")
        void changesTemplate() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot oldTemplate = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, oldTemplate, NOW);

            OffsetDateTime later = NOW.plusHours(1);
            ApprovalTemplateRoot newTemplate = ApprovalTemplateRoot.create(later);

            mapping.changeTemplate(newTemplate, later);

            assertThat(mapping.getApprovalTemplateRoot()).isEqualTo(newTemplate);
            assertThat(mapping.getUpdatedAt()).isEqualTo(later);
            assertThat(mapping.getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Given: 새 템플릿이 null이면 When: 템플릿을 변경하면 Then: 예외가 발생한다")
        void throwsWhenNewTemplateNull() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, template, NOW);

            assertThatThrownBy(() -> mapping.changeTemplate(null, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("승인선 템플릿");
        }
    }

    @Nested
    @DisplayName("기안 양식 템플릿 루트 관리")
    class DraftFormTemplateRootManagement {

        @Test
        @DisplayName("Given: 양식 템플릿 루트와 함께 생성하면 When: getDraftFormTemplateRoot Then: 루트 반환")
        void createsWithDraftFormTemplateRoot() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();
            DraftFormTemplateRoot formTemplateRoot = createDraftFormTemplateRoot();

            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, template, formTemplateRoot, NOW);

            assertThat(mapping.getDraftFormTemplateRoot()).isEqualTo(formTemplateRoot);
            assertThat(mapping.hasDraftFormTemplateRoot()).isTrue();
        }

        @Test
        @DisplayName("Given: 양식 템플릿 루트 없이 생성하면 When: hasDraftFormTemplateRoot Then: false 반환")
        void createsWithoutDraftFormTemplateRoot() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();

            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, template, NOW);

            assertThat(mapping.getDraftFormTemplateRoot()).isNull();
            assertThat(mapping.hasDraftFormTemplateRoot()).isFalse();
        }

        @Test
        @DisplayName("Given: 매핑이 있으면 When: changeDraftFormTemplateRoot Then: 루트 변경됨")
        void changesDraftFormTemplateRoot() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, template, NOW);

            OffsetDateTime later = NOW.plusHours(1);
            DraftFormTemplateRoot newRoot = createDraftFormTemplateRoot();

            mapping.changeDraftFormTemplateRoot(newRoot, later);

            assertThat(mapping.getDraftFormTemplateRoot()).isEqualTo(newRoot);
            assertThat(mapping.hasDraftFormTemplateRoot()).isTrue();
            assertThat(mapping.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("Given: 양식 템플릿 루트가 있으면 When: changeDraftFormTemplateRoot(null) Then: 루트 제거됨")
        void removesDraftFormTemplateRoot() {
            OrgGroup orgGroup = createOrgGroup();
            ApprovalTemplateRoot template = createTemplateRoot();
            DraftFormTemplateRoot formTemplateRoot = createDraftFormTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, template, formTemplateRoot, NOW);

            OffsetDateTime later = NOW.plusHours(1);
            mapping.changeDraftFormTemplateRoot(null, later);

            assertThat(mapping.getDraftFormTemplateRoot()).isNull();
            assertThat(mapping.hasDraftFormTemplateRoot()).isFalse();
            assertThat(mapping.getUpdatedAt()).isEqualTo(later);
        }
    }
}
