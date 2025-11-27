package com.example.draft.application.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;

class ApprovalLineTemplateRequestEqualityTest {

    @Test
    @DisplayName("ApprovalLineTemplateRequest equals/hashCode 동일/상이 케이스를 모두 커버한다")
    void equalsAndHashCode() {
        ApprovalTemplateStepRequest step1 = new ApprovalTemplateStepRequest(1, "GRP", "desc");
        ApprovalTemplateStepRequest step2 = new ApprovalTemplateStepRequest(1, "GRP", "desc");

        ApprovalLineTemplateRequest req1 = new ApprovalLineTemplateRequest("name", "HR", "ORG1", true, List.of(step1));
        ApprovalLineTemplateRequest req2 = new ApprovalLineTemplateRequest("name", "HR", "ORG1", true, List.of(step2));
        ApprovalLineTemplateRequest reqDifferent = new ApprovalLineTemplateRequest("name2", "HR", "ORG1", true, List.of(step2));

        assertThat(req1).isEqualTo(req2).hasSameHashCodeAs(req2);
        assertThat(req1).isNotEqualTo(reqDifferent);
    }
}
