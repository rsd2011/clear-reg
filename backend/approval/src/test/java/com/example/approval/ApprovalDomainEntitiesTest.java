package com.example.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.admin.approval.ApprovalAccessDeniedException;
import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.TemplateScope;
import org.junit.jupiter.api.Test;

class ApprovalDomainEntitiesTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Test
    void approvalGroupCreatesAndRenames() {
        ApprovalGroup group = ApprovalGroup.create("CODE", "이름", "desc", "ORG", null, NOW);
        group.rename("새 이름", "새 desc", "x > 1", NOW.plusSeconds(1));

        assertThat(group.getName()).isEqualTo("새 이름");
        assertThat(group.getConditionExpression()).isEqualTo("x > 1");
        assertThat(group.getUpdatedAt()).isAfter(NOW);
        assertThat(group.getOrganizationCode()).isEqualTo("ORG");
        assertThat(group.getDescription()).isEqualTo("새 desc");
    }

    @Test
    void approvalLineTemplateScopesAndOrganizationCheck() {
        ApprovalLineTemplate orgTemplate = ApprovalLineTemplate.create("템플릿", "HR", "ORG", NOW);
        assertThat(orgTemplate.getScope()).isEqualTo(TemplateScope.ORGANIZATION);
        assertThat(orgTemplate.applicableTo("ORG")).isTrue();
        assertThat(orgTemplate.applicableTo("OTHER")).isFalse();

        ApprovalLineTemplate globalTemplate = ApprovalLineTemplate.createGlobal("G", "HR", NOW);
        assertThat(globalTemplate.getScope()).isEqualTo(TemplateScope.GLOBAL);
        assertThat(globalTemplate.applicableTo("ANY")).isTrue();

        assertThatThrownBy(() -> orgTemplate.assertOrganization("OTHER"))
                .isInstanceOf(ApprovalAccessDeniedException.class);

        orgTemplate.addStep(1, "GRP1", "desc");
        orgTemplate.addStep(2, "GRP2", "desc2");
        assertThat(orgTemplate.getSteps()).hasSize(2);

        var newSteps = List.of(new com.example.admin.approval.ApprovalTemplateStep(orgTemplate, 3, "GRP3", "d3"));
        orgTemplate.replaceSteps(newSteps);
        assertThat(orgTemplate.getSteps()).hasSize(1);
        assertThat(orgTemplate.getSteps().getFirst().getApprovalGroupCode()).isEqualTo("GRP3");
    }

    @Test
    void approvalLineTemplateRenameUpdatesFlags() {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", "HR", "ORG", NOW);
        template.rename("새템플릿", false, NOW.plusSeconds(5));

        assertThat(template.getName()).isEqualTo("새템플릿");
        assertThat(template.isActive()).isFalse();
        assertThat(template.getUpdatedAt()).isAfter(NOW);
        assertThat(template.isGlobal()).isFalse();
    }

    @Test
    void approvalTemplateStepStoresValues() {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", "HR", "ORG", NOW);
        var step = new com.example.admin.approval.ApprovalTemplateStep(template, 2, "GRP2", "설명");
        assertThat(step.getTemplate()).isEqualTo(template);
        assertThat(step.getStepOrder()).isEqualTo(2);
        assertThat(step.getApprovalGroupCode()).isEqualTo("GRP2");
        assertThat(step.getDescription()).isEqualTo("설명");
    }
}
