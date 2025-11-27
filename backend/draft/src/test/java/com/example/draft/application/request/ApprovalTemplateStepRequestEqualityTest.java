package com.example.draft.application.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.dto.ApprovalTemplateStepRequest;

class ApprovalTemplateStepRequestEqualityTest {

    @Test
    @DisplayName("ApprovalTemplateStepRequest equals/hashCode 분기를 모두 커버한다")
    void equalsAndHashCode() {
        ApprovalTemplateStepRequest req1 = new ApprovalTemplateStepRequest(1, "GRP", "desc");
        ApprovalTemplateStepRequest req2 = new ApprovalTemplateStepRequest(1, "GRP", "desc");
        ApprovalTemplateStepRequest reqDiff = new ApprovalTemplateStepRequest(2, "GRP2", "other");

        assertThat(req1).isEqualTo(req2).hasSameHashCodeAs(req2);
        assertThat(req1).isNotEqualTo(reqDiff);
    }
}
