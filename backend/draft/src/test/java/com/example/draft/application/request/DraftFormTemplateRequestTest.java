package com.example.draft.application.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftFormTemplateRequestTest {

    @Test
    @DisplayName("DraftFormTemplateRequest는 전달된 필드를 그대로 보존한다")
    void preservesFields() {
        DraftFormTemplateRequest request = new DraftFormTemplateRequest("name", "HR", null, "{}", true);

        assertThat(request.name()).isEqualTo("name");
        assertThat(request.businessType()).isEqualTo("HR");
        assertThat(request.organizationCode()).isNull();
        assertThat(request.schemaJson()).isEqualTo("{}");
        assertThat(request.active()).isTrue();
    }
}
