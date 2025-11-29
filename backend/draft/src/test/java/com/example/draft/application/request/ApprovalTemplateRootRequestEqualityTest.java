package com.example.draft.application.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;

class ApprovalTemplateRootRequestEqualityTest {

    @Test
    @DisplayName("ApprovalTemplateRootRequest equals/hashCode 동일/상이 케이스를 모두 커버한다")
    void equalsAndHashCode() {
        ApprovalTemplateStepRequest step1 = new ApprovalTemplateStepRequest(1, "GRP");
        ApprovalTemplateStepRequest step2 = new ApprovalTemplateStepRequest(1, "GRP");

        ApprovalTemplateRootRequest req1 = new ApprovalTemplateRootRequest("name", 0, null, true, List.of(step1));
        ApprovalTemplateRootRequest req2 = new ApprovalTemplateRootRequest("name", 0, null, true, List.of(step2));
        ApprovalTemplateRootRequest reqDifferent = new ApprovalTemplateRootRequest("name2", 0, null, true, List.of(step2));

        assertThat(req1).isEqualTo(req2).hasSameHashCodeAs(req2);
        assertThat(req1).isNotEqualTo(reqDifferent);
    }
}
