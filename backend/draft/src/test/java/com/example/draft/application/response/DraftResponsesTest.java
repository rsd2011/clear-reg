package com.example.draft.application.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.TemplateScope;

class DraftResponsesTest {

    @Test
    @DisplayName("ApprovalGroupResponse apply/from에 마스킹 함수가 적용된다")
    void approvalGroupResponseApplyMasks() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("G1","Name","Desc","ORG","expr", now);
        ApprovalGroupResponse resp = ApprovalGroupResponse.from(group, mask());
        assertThat(resp.name()).isEqualTo("x");
        ApprovalGroupResponse again = ApprovalGroupResponse.apply(resp, mask());
        assertThat(again.groupCode()).isEqualTo("x");
    }

    @Test
    @DisplayName("ApprovalLineTemplateResponse apply/from에 마스킹 함수가 적용된다")
    void approvalLineTemplateResponseApplyMasks() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate template = ApprovalLineTemplate.create("Name","BT","ORG", now);
        ApprovalLineTemplateResponse resp = ApprovalLineTemplateResponse.from(template, mask());
        assertThat(resp.name()).isEqualTo("x");
        ApprovalLineTemplateResponse again = ApprovalLineTemplateResponse.apply(resp, mask());
        assertThat(again.templateCode()).isEqualTo("x");
    }

    @Test
    @DisplayName("DraftFormTemplateResponse apply/from에 마스킹 함수가 적용된다")
    void draftFormTemplateResponseApplyMasks() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate template = DraftFormTemplate.create("Form","BT","ORG","{}", now);
        DraftFormTemplateResponse resp = DraftFormTemplateResponse.from(template, mask());
        assertThat(resp.name()).isEqualTo("x");
        DraftFormTemplateResponse again = DraftFormTemplateResponse.apply(resp, mask());
        assertThat(again.templateCode()).isEqualTo("x");
    }

    private UnaryOperator<String> mask() { return s -> "x"; }
}
