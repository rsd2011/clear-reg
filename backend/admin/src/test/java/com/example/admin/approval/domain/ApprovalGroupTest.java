package com.example.admin.approval.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.example.admin.approval.exception.ApprovalAccessDeniedException;
import com.example.common.version.ChangeAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("결재선 템플릿/결재그룹 도메인 테스트")
class ApprovalGroupTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Nested
    @DisplayName("ApprovalGroup 테스트")
    class ApprovalGroupTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When create 호출 Then ApprovalGroup이 생성된다")
        void createApprovalGroup() {
            ApprovalGroup group = ApprovalGroup.create("G1", "결재그룹1", "설명", 10, NOW);

            assertThat(group.getGroupCode()).isEqualTo("G1");
            assertThat(group.getName()).isEqualTo("결재그룹1");
            assertThat(group.getDescription()).isEqualTo("설명");
            assertThat(group.getDisplayOrder()).isEqualTo(10);
            assertThat(group.getCreatedAt()).isEqualTo(NOW);
            assertThat(group.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Given displayOrder가 null When create 호출 Then 기본값 0이 설정된다")
        void createApprovalGroupWithNullDisplayOrder() {
            ApprovalGroup group = ApprovalGroup.create("G1", "결재그룹1", "설명", null, NOW);

            assertThat(group.getDisplayOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("Given 기존 그룹 When rename 호출 Then 필드가 갱신된다")
        void renameApprovalGroup() {
            ApprovalGroup group = ApprovalGroup.create("G1", "결재그룹1", "설명", 10, NOW);
            OffsetDateTime later = NOW.plusHours(1);

            group.rename("변경된이름", "변경된설명", later);

            assertThat(group.getName()).isEqualTo("변경된이름");
            assertThat(group.getDescription()).isEqualTo("변경된설명");
            assertThat(group.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("Given 기존 그룹 When updateDisplayOrder 호출 Then displayOrder가 갱신된다")
        void updateDisplayOrderApprovalGroup() {
            ApprovalGroup group = ApprovalGroup.create("G1", "결재그룹1", "설명", 0, NOW);
            OffsetDateTime later = NOW.plusHours(1);

            group.updateDisplayOrder(20, later);

            assertThat(group.getDisplayOrder()).isEqualTo(20);
            assertThat(group.getUpdatedAt()).isEqualTo(later);
        }
    }

    @Nested
    @DisplayName("ApprovalTemplateRoot 테스트")
    class ApprovalTemplateRootTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When create 호출 Then ApprovalTemplateRoot이 생성된다")
        void createApprovalTemplateRoot() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);

            assertThat(root.getTemplateCode()).isNotNull();
            assertThat(root.getCreatedAt()).isEqualTo(NOW);
            assertThat(root.getUpdatedAt()).isEqualTo(NOW);
            assertThat(root.getCurrentVersion()).isNull();
        }

        @Test
        @DisplayName("Given Root와 버전 When activateNewVersion 호출 Then 현재 버전이 설정된다")
        void activateNewVersionSetsCurrentVersion() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);
            ApprovalTemplate version = ApprovalTemplate.create(
                    root, 1, "템플릿1", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", NOW);

            root.activateNewVersion(version, NOW);

            assertThat(root.getCurrentVersion()).isEqualTo(version);
            assertThat(root.getName()).isEqualTo("템플릿1");
            assertThat(root.getDescription()).isEqualTo("설명");
            assertThat(root.getDisplayOrder()).isEqualTo(0);
            assertThat(root.isActive()).isTrue();
        }

        @Test
        @DisplayName("Given 버전이 없을 때 When 편의 메서드 호출 Then null 또는 기본값 반환")
        void convenienceMethodsReturnDefaultWhenNoVersion() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);

            assertThat(root.getName()).isNull();
            assertThat(root.getDescription()).isNull();
            assertThat(root.getDisplayOrder()).isEqualTo(0);
            assertThat(root.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("ApprovalTemplate 테스트")
    class ApprovalTemplateTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When create 호출 Then ApprovalTemplate이 생성된다")
        void createApprovalTemplate() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);
            ApprovalTemplate template = ApprovalTemplate.create(
                    root, 1, "템플릿1", 10, "설명", true,
                    ChangeAction.CREATE, "생성 사유", "user", "사용자", NOW);

            assertThat(template.getRoot()).isEqualTo(root);
            assertThat(template.getVersion()).isEqualTo(1);
            assertThat(template.getName()).isEqualTo("템플릿1");
            assertThat(template.getDisplayOrder()).isEqualTo(10);
            assertThat(template.getDescription()).isEqualTo("설명");
            assertThat(template.isActive()).isTrue();
            assertThat(template.getChangeAction()).isEqualTo(ChangeAction.CREATE);
        }

        @Test
        @DisplayName("Given 템플릿 When addStep 호출 Then 단계가 추가된다")
        void addStepToTemplate() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);
            ApprovalTemplate template = ApprovalTemplate.create(
                    root, 1, "템플릿1", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", NOW);
            ApprovalGroup group = ApprovalGroup.create("GRP1", "그룹1", "설명", 1, NOW);
            ApprovalTemplateStep step = ApprovalTemplateStep.create(template, 1, group, false);

            template.addStep(step);

            assertThat(template.getSteps()).hasSize(1);
            assertThat(template.getSteps().get(0).getStepOrder()).isEqualTo(1);
            assertThat(template.getSteps().get(0).getApprovalGroup()).isEqualTo(group);
        }
    }

    @Nested
    @DisplayName("ApprovalTemplateStep 테스트")
    class ApprovalTemplateStepTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When create 호출 Then ApprovalTemplateStep이 생성된다")
        void createApprovalTemplateStep() {
            ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);
            ApprovalTemplate template = ApprovalTemplate.create(
                    root, 1, "템플릿1", 0, "설명", true,
                    ChangeAction.CREATE, null, "user", "사용자", NOW);
            ApprovalGroup group = ApprovalGroup.create("GRP1", "그룹1", "설명", 1, NOW);

            ApprovalTemplateStep step = ApprovalTemplateStep.create(template, 1, group, false);

            assertThat(step.getStepOrder()).isEqualTo(1);
            assertThat(step.getApprovalGroup()).isEqualTo(group);
            assertThat(step.getTemplate()).isEqualTo(template);
            assertThat(step.getApprovalGroupCode()).isEqualTo("GRP1");
            assertThat(step.getApprovalGroupName()).isEqualTo("그룹1");
        }
    }

    @Nested
    @DisplayName("ApprovalAccessDeniedException 테스트")
    class ApprovalAccessDeniedExceptionTests {

        @Test
        @DisplayName("Given 메시지 When 예외 생성 Then 메시지가 보존된다")
        void exceptionPreservesMessage() {
            String message = "접근이 거부되었습니다.";
            ApprovalAccessDeniedException ex = new ApprovalAccessDeniedException(message);

            assertThat(ex.getMessage()).isEqualTo(message);
        }
    }
}
