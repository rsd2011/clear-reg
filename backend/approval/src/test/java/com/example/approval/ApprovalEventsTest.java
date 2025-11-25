package com.example.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.example.approval.api.ApprovalStatus;
import com.example.approval.api.event.ApprovalCompletedEvent;
import com.example.approval.api.event.DraftSubmittedEvent;
import org.junit.jupiter.api.Test;

class ApprovalEventsTest {

    @Test
    void draftSubmittedEventStoresFields() {
        UUID draftId = UUID.randomUUID();
        DraftSubmittedEvent event = new DraftSubmittedEvent(draftId, "TMP", "ORG", "user", "summary", List.of("G1"));

        assertThat(event.draftId()).isEqualTo(draftId);
        assertThat(event.approvalGroupCodes()).containsExactly("G1");
    }

    @Test
    void approvalCompletedEventStoresFields() {
        UUID draftId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();
        ApprovalCompletedEvent event = new ApprovalCompletedEvent(approvalId, draftId, ApprovalStatus.APPROVED, "actor", "ok");

        assertThat(event.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(event.draftId()).isEqualTo(draftId);
        assertThat(event.approvalRequestId()).isEqualTo(approvalId);
    }
}
