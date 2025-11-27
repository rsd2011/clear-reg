package com.example.admin.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("결재선 템플릿/결재그룹 도메인 테스트")
class ApprovalDomainTest {

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
    @DisplayName("ApprovalLineTemplate 테스트")
    class ApprovalLineTemplateTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When create 호출 Then ApprovalLineTemplate이 생성된다")
        void createApprovalLineTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", 0, "설명", NOW);

            assertThat(template.getName()).isEqualTo("템플릿1");
            assertThat(template.getDisplayOrder()).isEqualTo(0);
            assertThat(template.getDescription()).isEqualTo("설명");
            assertThat(template.isActive()).isTrue();
            assertThat(template.getTemplateCode()).isNotNull();
        }

        @Test
        @DisplayName("Given 기존 템플릿 When rename 호출 Then 필드가 갱신된다")
        void renameApprovalLineTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", 0, null, NOW);
            OffsetDateTime later = NOW.plusHours(1);

            template.rename("변경된이름", 5, "변경된설명", false, later);

            assertThat(template.getName()).isEqualTo("변경된이름");
            assertThat(template.getDisplayOrder()).isEqualTo(5);
            assertThat(template.getDescription()).isEqualTo("변경된설명");
            assertThat(template.isActive()).isFalse();
            assertThat(template.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("Given 템플릿 When addStep 호출 Then 단계가 추가된다")
        void addStepToTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", 0, null, NOW);
            ApprovalGroup group = ApprovalGroup.create("GRP1", "그룹1", "설명", 1, NOW);

            template.addStep(1, group);

            assertThat(template.getSteps()).hasSize(1);
            assertThat(template.getSteps().get(0).getStepOrder()).isEqualTo(1);
            assertThat(template.getSteps().get(0).getApprovalGroup()).isEqualTo(group);
        }

        @Test
        @DisplayName("Given 템플릿에 단계 추가 후 When replaceSteps 호출 Then 단계가 교체된다")
        void replaceSteps() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", 0, null, NOW);
            ApprovalGroup group1 = ApprovalGroup.create("GRP1", "그룹1", "설명", 1, NOW);
            ApprovalGroup group2 = ApprovalGroup.create("GRP2", "그룹2", "설명", 2, NOW);
            ApprovalGroup group3 = ApprovalGroup.create("GRP3", "그룹3", "설명", 3, NOW);

            template.addStep(1, group1);
            template.addStep(2, group2);

            ApprovalTemplateStep newStep = new ApprovalTemplateStep(template, 1, group3);
            template.replaceSteps(List.of(newStep));

            assertThat(template.getSteps()).hasSize(1);
            assertThat(template.getSteps().get(0).getApprovalGroup()).isEqualTo(group3);
        }
    }

    @Nested
    @DisplayName("ApprovalTemplateStep 테스트")
    class ApprovalTemplateStepTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When 생성자 호출 Then ApprovalTemplateStep이 생성된다")
        void createApprovalTemplateStep() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", 0, null, NOW);
            ApprovalGroup group = ApprovalGroup.create("GRP1", "그룹1", "설명", 1, NOW);
            ApprovalTemplateStep step = new ApprovalTemplateStep(template, 1, group);

            assertThat(step.getStepOrder()).isEqualTo(1);
            assertThat(step.getApprovalGroup()).isEqualTo(group);
            assertThat(step.getTemplate()).isEqualTo(template);
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
