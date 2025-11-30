package com.example.draft.application.request;
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.orggroup.WorkType;

class DraftFormTemplateRequestTest {

    @Test
    @DisplayName("DraftFormTemplateRequest는 전달된 필드를 그대로 보존한다")
    void preservesFields() {
        DraftFormTemplateRequest request = new DraftFormTemplateRequest(
                "name", WorkType.HR_UPDATE, "{}", true, null);

        assertThat(request.name()).isEqualTo("name");
        assertThat(request.workType()).isEqualTo(WorkType.HR_UPDATE);
        assertThat(request.schemaJson()).isEqualTo("{}");
        assertThat(request.active()).isTrue();
    }
}
