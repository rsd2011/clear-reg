package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;

class BusinessTemplateMappingTest {

    private ApprovalTemplateRoot createTemplateRoot(String name, OffsetDateTime now) {
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        root.activateNewVersion(version, now);
        return root;
    }

    private DraftFormTemplate createFormTemplate(String name, OffsetDateTime now) {
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, name, WorkType.HR_UPDATE, "{}", true,
                ChangeAction.CREATE, null, "system", "System", now);
        // create()는 이미 PUBLISHED 상태로 생성됨
        root.activateNewVersion(template, now);
        return template;
    }

    @Test
    @DisplayName("BusinessTemplateMapping은 생성 후 applicableTo로 조직/글로벌 매핑을 판별한다")
    void applicableToOrgAndGlobal() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot line = createTemplateRoot("code", now);
        DraftFormTemplate form = createFormTemplate("form", now);

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
        ApprovalTemplateRoot line = createTemplateRoot("code", now);
        DraftFormTemplate form = createFormTemplate("form", now);
        BusinessTemplateMapping mapping = BusinessTemplateMapping.create("HR", "ORG1", line, form, now);

        ApprovalTemplateRoot newLine = createTemplateRoot("code2", now.plusSeconds(1));
        DraftFormTemplate newForm = createFormTemplate("form2", now.plusSeconds(1));

        mapping.updateTemplates(newLine, newForm, false, now.plusSeconds(2));

        assertThat(mapping.getApprovalTemplateRoot()).isEqualTo(newLine);
        assertThat(mapping.getDraftFormTemplate()).isEqualTo(newForm);
        assertThat(mapping.isActive()).isFalse();
        assertThat(mapping.getUpdatedAt()).isAfter(mapping.getCreatedAt());
    }
}
