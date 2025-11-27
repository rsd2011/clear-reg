package com.example.admin.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            ApprovalGroup group = ApprovalGroup.create("G1", "결재그룹1", "설명", "ORG1", "expr", NOW);

            assertThat(group.getGroupCode()).isEqualTo("G1");
            assertThat(group.getName()).isEqualTo("결재그룹1");
            assertThat(group.getDescription()).isEqualTo("설명");
            assertThat(group.getOrganizationCode()).isEqualTo("ORG1");
            assertThat(group.getConditionExpression()).isEqualTo("expr");
            assertThat(group.getCreatedAt()).isEqualTo(NOW);
            assertThat(group.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Given 기존 그룹 When rename 호출 Then 필드가 갱신된다")
        void renameApprovalGroup() {
            ApprovalGroup group = ApprovalGroup.create("G1", "결재그룹1", "설명", "ORG1", "expr", NOW);
            OffsetDateTime later = NOW.plusHours(1);

            group.rename("변경된이름", "변경된설명", "newExpr", later);

            assertThat(group.getName()).isEqualTo("변경된이름");
            assertThat(group.getDescription()).isEqualTo("변경된설명");
            assertThat(group.getConditionExpression()).isEqualTo("newExpr");
            assertThat(group.getUpdatedAt()).isEqualTo(later);
        }
    }

    @Nested
    @DisplayName("ApprovalLineTemplate 테스트")
    class ApprovalLineTemplateTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When create 호출 Then ApprovalLineTemplate이 생성된다")
        void createApprovalLineTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", "HR", "ORG1", NOW);

            assertThat(template.getName()).isEqualTo("템플릿1");
            assertThat(template.getBusinessType()).isEqualTo("HR");
            assertThat(template.getOrganizationCode()).isEqualTo("ORG1");
            assertThat(template.isActive()).isTrue();
            assertThat(template.getScope()).isEqualTo(TemplateScope.ORGANIZATION);
            assertThat(template.getTemplateCode()).isNotNull();
        }

        @Test
        @DisplayName("Given organizationCode가 null When create 호출 Then scope가 GLOBAL이다")
        void createGlobalTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("전역템플릿", "HR", null, NOW);

            assertThat(template.getScope()).isEqualTo(TemplateScope.GLOBAL);
            assertThat(template.getOrganizationCode()).isNull();
        }

        @Test
        @DisplayName("Given 기존 템플릿 When rename 호출 Then 필드가 갱신된다")
        void renameApprovalLineTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", "HR", "ORG1", NOW);
            OffsetDateTime later = NOW.plusHours(1);

            template.rename("변경된이름", false, later);

            assertThat(template.getName()).isEqualTo("변경된이름");
            assertThat(template.isActive()).isFalse();
            assertThat(template.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("Given 템플릿 When addStep 호출 Then 단계가 추가된다")
        void addStepToTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", "HR", "ORG1", NOW);

            template.addStep(1, "GRP1", "1단계");

            assertThat(template.getSteps()).hasSize(1);
            assertThat(template.getSteps().get(0).getStepOrder()).isEqualTo(1);
            assertThat(template.getSteps().get(0).getApprovalGroupCode()).isEqualTo("GRP1");
        }

        @Test
        @DisplayName("Given 템플릿에 단계 추가 후 When replaceSteps 호출 Then 단계가 교체된다")
        void replaceSteps() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", "HR", "ORG1", NOW);
            template.addStep(1, "GRP1", "1단계");
            template.addStep(2, "GRP2", "2단계");

            ApprovalTemplateStep newStep = new ApprovalTemplateStep(template, 1, "GRP3", "새로운단계");
            template.replaceSteps(List.of(newStep));

            assertThat(template.getSteps()).hasSize(1);
            assertThat(template.getSteps().get(0).getApprovalGroupCode()).isEqualTo("GRP3");
        }

        @Test
        @DisplayName("Given GLOBAL 템플릿 When isGlobal 호출 Then true 반환")
        void isGlobalTemplate() {
            ApprovalLineTemplate template = ApprovalLineTemplate.createGlobal("전역템플릿", "HR", NOW);

            assertThat(template.isGlobal()).isTrue();
        }

        @Test
        @DisplayName("Given ORGANIZATION 템플릿 When applicableTo 호출 Then 조직코드 일치 여부 반환")
        void applicableToOrganization() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", "HR", "ORG1", NOW);

            assertThat(template.applicableTo("ORG1")).isTrue();
            assertThat(template.applicableTo("ORG2")).isFalse();
        }

        @Test
        @DisplayName("Given GLOBAL 템플릿 When applicableTo 호출 Then 항상 true 반환")
        void globalApplicableToAnyOrg() {
            ApprovalLineTemplate template = ApprovalLineTemplate.createGlobal("전역템플릿", "HR", NOW);

            assertThat(template.applicableTo("ORG1")).isTrue();
            assertThat(template.applicableTo("ORG2")).isTrue();
        }

        @Test
        @DisplayName("Given 다른 조직 코드 When assertOrganization 호출 Then 예외 발생")
        void assertOrganizationThrowsException() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", "HR", "ORG1", NOW);

            assertThatThrownBy(() -> template.assertOrganization("ORG2"))
                    .isInstanceOf(ApprovalAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("ApprovalTemplateStep 테스트")
    class ApprovalTemplateStepTests {

        @Test
        @DisplayName("Given 유효한 파라미터 When 생성자 호출 Then ApprovalTemplateStep이 생성된다")
        void createApprovalTemplateStep() {
            ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿1", "HR", "ORG1", NOW);
            ApprovalTemplateStep step = new ApprovalTemplateStep(template, 1, "GRP1", "1단계");

            assertThat(step.getStepOrder()).isEqualTo(1);
            assertThat(step.getApprovalGroupCode()).isEqualTo("GRP1");
            assertThat(step.getDescription()).isEqualTo("1단계");
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

    @Nested
    @DisplayName("TemplateScope 테스트")
    class TemplateScopeTests {

        @Test
        @DisplayName("Given TemplateScope When values 호출 Then GLOBAL, ORGANIZATION이 반환된다")
        void templateScopeValues() {
            TemplateScope[] values = TemplateScope.values();

            assertThat(values).containsExactlyInAnyOrder(TemplateScope.GLOBAL, TemplateScope.ORGANIZATION);
        }

        @Test
        @DisplayName("Given 문자열 When valueOf 호출 Then 해당 enum이 반환된다")
        void templateScopeValueOf() {
            assertThat(TemplateScope.valueOf("GLOBAL")).isEqualTo(TemplateScope.GLOBAL);
            assertThat(TemplateScope.valueOf("ORGANIZATION")).isEqualTo(TemplateScope.ORGANIZATION);
        }
    }
}
