package com.example.draft.application.approval;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.approval.api.ApprovalStatus;
import com.example.approval.api.event.ApprovalCompletedEvent;
import com.example.draft.application.port.out.ApprovalResultPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

class ApprovalCompletedEventListenerTest {

    @Test
    void delegatesToResultPort() {
        ApprovalResultPort port = Mockito.mock(ApprovalResultPort.class);
        ApprovalCompletedEventListener listener = new ApprovalCompletedEventListener(port);

        listener.handle(new ApprovalCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), ApprovalStatus.APPROVED, "actor", "ok"));

        verify(port).onApprovalCompleted(any(), any(), any(), any(), any());
    }
}
