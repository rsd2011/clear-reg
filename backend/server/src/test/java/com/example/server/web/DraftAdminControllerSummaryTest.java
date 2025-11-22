package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.TemplateScope;

class DraftAdminControllerSummaryTest {

    @Test
    @DisplayName("ApprovalLineTemplateSummary.from가 필요한 필드를 변환한다")
    void approvalLineTemplateSummary() {
        ApprovalLineTemplate template = mock(ApprovalLineTemplate.class);
        UUID id = UUID.randomUUID();
        when(template.getId()).thenReturn(id);
        when(template.getTemplateCode()).thenReturn("TPL-1");
        when(template.getName()).thenReturn("결재선");
        when(template.getBusinessType()).thenReturn("HR");
        when(template.getScope()).thenReturn(TemplateScope.ORGANIZATION);
        when(template.getOrganizationCode()).thenReturn("ORG1");
        when(template.isActive()).thenReturn(true);

        DraftAdminController.ApprovalLineTemplateSummary summary = DraftAdminController.ApprovalLineTemplateSummary.from(template, java.util.function.UnaryOperator.identity());

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.templateCode()).isEqualTo("TPL-1");
        assertThat(summary.scope()).isEqualTo(TemplateScope.ORGANIZATION.name());
    }

    @Test
    @DisplayName("DraftFormTemplateSummary.from가 필드를 변환한다")
    void draftFormTemplateSummary() {
        DraftFormTemplate template = mock(DraftFormTemplate.class);
        UUID id = UUID.randomUUID();
        when(template.getId()).thenReturn(id);
        when(template.getTemplateCode()).thenReturn("FORM-1");
        when(template.getName()).thenReturn("양식");
        when(template.getBusinessType()).thenReturn("HR");
        when(template.getScope()).thenReturn(TemplateScope.GLOBAL);
        when(template.getOrganizationCode()).thenReturn("ORG1");
        when(template.isActive()).thenReturn(false);
        when(template.getVersion()).thenReturn(2);

        DraftAdminController.DraftFormTemplateSummary summary = DraftAdminController.DraftFormTemplateSummary.from(template, java.util.function.UnaryOperator.identity());

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.active()).isFalse();
        assertThat(summary.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("ApprovalGroupSummary.from는 그룹 정보를 요약한다")
    void approvalGroupSummary() {
        ApprovalGroup group = mock(ApprovalGroup.class);
        UUID id = UUID.randomUUID();
        when(group.getId()).thenReturn(id);
        when(group.getGroupCode()).thenReturn("GRP");
        when(group.getName()).thenReturn("조직");
        when(group.getOrganizationCode()).thenReturn("ORG1");

        DraftAdminController.ApprovalGroupSummary summary = DraftAdminController.ApprovalGroupSummary.from(group, java.util.function.UnaryOperator.identity());

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.groupCode()).isEqualTo("GRP");
        assertThat(summary.organizationCode()).isEqualTo("ORG1");
    }
}
