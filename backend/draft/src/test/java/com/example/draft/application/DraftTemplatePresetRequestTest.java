package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.application.request.DraftTemplatePresetRequest;

class DraftTemplatePresetRequestTest {

    @Test
    @DisplayName("DraftTemplatePresetRequest는 값들을 그대로 보존하고 리스트를 불변으로 만든다")
    void preservesValuesAndMakesVariablesImmutable() {
        UUID formId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();
        List<String> vars = List.of("작성자", "custom");

        DraftTemplatePresetRequest req = new DraftTemplatePresetRequest(
                "사전기안",
                "NOTICE",
                "ORG1",
                "{작성자} 제목",
                "내용",
                formId,
                approvalId,
                "{\"field\":true}",
                vars,
                true);

        assertThat(req.name()).isEqualTo("사전기안");
        assertThat(req.businessFeatureCode()).isEqualTo("NOTICE");
        assertThat(req.organizationCode()).isEqualTo("ORG1");
        assertThat(req.formTemplateId()).isEqualTo(formId);
        assertThat(req.defaultApprovalTemplateId()).isEqualTo(approvalId);
        assertThat(req.defaultFormPayload()).contains("field");
        assertThat(req.variables()).containsExactlyElementsOf(vars);
        assertThat(req.active()).isTrue();

        // 불변 리스트 확인
        assertThat(req.variables()).isUnmodifiable();
    }
}
