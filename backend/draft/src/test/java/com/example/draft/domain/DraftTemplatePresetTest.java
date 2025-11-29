package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.common.version.ChangeAction;
import com.example.draft.domain.exception.DraftAccessDeniedException;

class DraftTemplatePresetTest {

    private final OffsetDateTime now = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private ApprovalTemplateRoot createTemplateRoot(String name) {
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        root.activateNewVersion(version, now);
        return root;
    }

    @Test
    @DisplayName("조직 프리셋과 글로벌 프리셋 생성/속성/매칭 확인")
    void createAndAttributes() {
        DraftFormTemplate formOrg = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        ApprovalTemplateRoot approvalOrg = createTemplateRoot("결재");
        DraftTemplatePreset orgPreset = DraftTemplatePreset.create("이름", "NOTICE", "ORG1",
                "제목", "내용", formOrg, approvalOrg, "{}", "[]", true, now);

        assertThat(orgPreset.isGlobal()).isFalse();
        assertThat(orgPreset.matchesBusiness("NOTICE")).isTrue();
        assertThat(orgPreset.getDefaultApprovalTemplate()).isEqualTo(approvalOrg);

        DraftFormTemplate formGlobal = DraftFormTemplate.create("폼G", "NOTICE", null, "{}", now);
        DraftTemplatePreset globalPreset = DraftTemplatePreset.create("글로벌", "NOTICE", null,
                "제목", "내용", formGlobal, null, "{}", "[]", true, now);
        assertThat(globalPreset.isGlobal()).isTrue();
    }

    @Test
    @DisplayName("조직 스코프 검증 실패 시 예외 발생")
    void assertOrganizationFails() {
        DraftFormTemplate formOrg = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        DraftTemplatePreset preset = DraftTemplatePreset.create("이름", "NOTICE", "ORG1",
                "제목", "내용", formOrg, null, "{}", "[]", true, now);

        assertThatThrownBy(() -> preset.assertOrganization("OTHER"))
                .isInstanceOf(DraftAccessDeniedException.class);
    }

    @Test
    @DisplayName("update는 필드와 버전을 갱신한다")
    void updatePreset() {
        DraftFormTemplate form = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        DraftTemplatePreset preset = DraftTemplatePreset.create("이름", "NOTICE", "ORG1",
                "제목", "내용", form, null, "{}", "[]", true, now);
        DraftFormTemplate newForm = DraftFormTemplate.create("폼2", "NOTICE", "ORG1", "{}", now.plusDays(1));
        ApprovalTemplateRoot approval = createTemplateRoot("결재");

        preset.update("새이름", "새제목", "새내용", newForm, approval, "{\"x\":1}", "[\"v\"]", false, now.plusDays(2));

        assertThat(preset.getName()).isEqualTo("새이름");
        assertThat(preset.getFormTemplate()).isEqualTo(newForm);
        assertThat(preset.getDefaultApprovalTemplate()).isEqualTo(approval);
        assertThat(preset.getVersion()).isEqualTo(2);
        assertThat(preset.isActive()).isFalse();
    }
}
