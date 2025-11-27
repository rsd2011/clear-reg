package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalLineTemplate;
import com.example.draft.application.response.DraftTemplatePresetResponse;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.DraftTemplatePreset;

class DraftTemplatePresetResponseTest {

    @Test
    @DisplayName("DraftTemplatePresetResponse.from/apply는 필드를 그대로 매핑하고 마스킹 함수를 적용한다")
    void responseFromAndApply() {
        OffsetDateTime now = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        DraftFormTemplate form = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        ApprovalLineTemplate approval = ApprovalLineTemplate.create("결재", "NOTICE", "ORG1", now);
        DraftTemplatePreset preset = DraftTemplatePreset.create(
                "사전기안",
                "NOTICE",
                "ORG1",
                "{작성자} 제목",
                "내용",
                form,
                approval,
                "{\"field\":true}",
                "[\"작성자\"]",
                true,
                now);

        DraftTemplatePresetResponse response = DraftTemplatePresetResponse.from(preset, List.of("작성자"), s -> "MASK:" + s);

        assertThat(response.presetCode()).startsWith("MASK:");
        assertThat(response.name()).startsWith("MASK:");
        assertThat(response.formTemplateId()).isEqualTo(form.getId());
        assertThat(response.defaultApprovalTemplateId()).isEqualTo(approval.getId());
        assertThat(response.variables()).containsExactly("작성자");

        DraftTemplatePresetResponse applied = DraftTemplatePresetResponse.apply(response, s -> s.replace("MASK:", ""));
        assertThat(applied.presetCode()).doesNotContain("MASK:");
        assertThat(applied.name()).isEqualTo(preset.getName());
    }
}
