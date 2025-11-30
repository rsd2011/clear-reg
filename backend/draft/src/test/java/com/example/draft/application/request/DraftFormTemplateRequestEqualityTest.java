package com.example.draft.application.request;
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.orggroup.WorkType;

class DraftFormTemplateRequestEqualityTest {

    @Test
    @DisplayName("DraftFormTemplateRequest equals/hashCode 분기 커버")
    void equalsAndHashCode() {
        DraftFormTemplateRequest r1 = new DraftFormTemplateRequest(
                "name", WorkType.HR_UPDATE, "{}", true, null);
        DraftFormTemplateRequest r2 = new DraftFormTemplateRequest(
                "name", WorkType.HR_UPDATE, "{}", true, null);
        DraftFormTemplateRequest rDiff = new DraftFormTemplateRequest(
                "name2", WorkType.HR_UPDATE, "{}", true, null);

        assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
        assertThat(r1).isNotEqualTo(rDiff);
    }
}
