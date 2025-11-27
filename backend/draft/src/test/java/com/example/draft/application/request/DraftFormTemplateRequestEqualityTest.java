package com.example.draft.application.request;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftFormTemplateRequestEqualityTest {

    @Test
    @DisplayName("DraftFormTemplateRequest equals/hashCode 분기 커버")
    void equalsAndHashCode() {
        DraftFormTemplateRequest r1 = new DraftFormTemplateRequest("name", "HR", "ORG1", "{}", true);
        DraftFormTemplateRequest r2 = new DraftFormTemplateRequest("name", "HR", "ORG1", "{}", true);
        DraftFormTemplateRequest rDiff = new DraftFormTemplateRequest("name2", "HR", "ORG1", "{}", true);

        assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
        assertThat(r1).isNotEqualTo(rDiff);
    }
}
