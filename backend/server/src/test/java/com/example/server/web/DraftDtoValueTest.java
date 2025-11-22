package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftDtoValueTest {

    @Test
    @DisplayName("ApprovalGroupSummary는 필드를 보존한다")
    void approvalGroupSummaryStoresFields() {
        UUID id = UUID.randomUUID();
        DraftAdminController.ApprovalGroupSummary dto = new DraftAdminController.ApprovalGroupSummary(id, "GRP", "name", "ORG", true);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.groupCode()).isEqualTo("GRP");
        assertThat(dto.organizationCode()).isEqualTo("ORG");
        assertThat(dto.active()).isTrue();
    }

    @Test
    @DisplayName("ApprovalLineTemplateSummary는 필드를 보존한다")
    void approvalLineTemplateSummaryStoresFields() {
        UUID id = UUID.randomUUID();
        DraftAdminController.ApprovalLineTemplateSummary dto = new DraftAdminController.ApprovalLineTemplateSummary(id, "CODE", "nm", "BT", "SCOPE", "ORG", true);

        assertThat(dto.templateCode()).isEqualTo("CODE");
        assertThat(dto.scope()).isEqualTo("SCOPE");
        assertThat(dto.active()).isTrue();
    }

    @Test
    @DisplayName("DraftFormTemplateSummary는 필드를 보존한다")
    void draftFormTemplateSummaryStoresFields() {
        UUID id = UUID.randomUUID();
        DraftAdminController.DraftFormTemplateSummary dto = new DraftAdminController.DraftFormTemplateSummary(id, "CODE", "nm", "BT", "SCOPE", "ORG", true, 2);

        assertThat(dto.version()).isEqualTo(2);
        assertThat(dto.organizationCode()).isEqualTo("ORG");
    }
}
