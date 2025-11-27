package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalLineTemplate;

class BusinessTemplateMappingTest {

    @Test
    @DisplayName("BusinessTemplateMapping은 생성 후 applicableTo로 조직/글로벌 매핑을 판별한다")
    void applicableToOrgAndGlobal() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate line = ApprovalLineTemplate.create("code", 0, null, now);
        DraftFormTemplate form = DraftFormTemplate.create("form", "HR", "ORG1", "{}", now);

        BusinessTemplateMapping orgMapping = BusinessTemplateMapping.create("HR", "ORG1", line, form, now);
        BusinessTemplateMapping globalMapping = BusinessTemplateMapping.create("HR", null, line, form, now);

        assertThat(orgMapping.applicableTo("HR", "ORG1")).isTrue();
        assertThat(orgMapping.applicableTo("HR", "OTHER")).isFalse();
        assertThat(globalMapping.applicableTo("HR", "ANY"))
                .as("organizationCode가 null이면 모든 조직에 적용")
                .isTrue();
    }

    @Test
    @DisplayName("updateTemplates는 approval/form 템플릿과 active 플래그, updatedAt을 갱신한다")
    void updatesTemplatesAndActiveFlag() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate line = ApprovalLineTemplate.create("code", 0, null, now);
        DraftFormTemplate form = DraftFormTemplate.create("form", "HR", "ORG1", "{}", now);
        BusinessTemplateMapping mapping = BusinessTemplateMapping.create("HR", "ORG1", line, form, now);

        ApprovalLineTemplate newLine = ApprovalLineTemplate.create("code2", 0, null, now.plusSeconds(1));
        DraftFormTemplate newForm = DraftFormTemplate.create("form2", "HR", "ORG1", "{}", now.plusSeconds(1));

        mapping.updateTemplates(newLine, newForm, false, now.plusSeconds(2));

        assertThat(mapping.getApprovalLineTemplate()).isEqualTo(newLine);
        assertThat(mapping.getDraftFormTemplate()).isEqualTo(newForm);
        assertThat(mapping.isActive()).isFalse();
        assertThat(mapping.getUpdatedAt()).isAfter(mapping.getCreatedAt());
    }
}
