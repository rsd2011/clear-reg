package com.example.draft.application.approval;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.approval.api.event.ApprovalCompletedEvent;
import com.example.draft.application.port.out.ApprovalResultPort;

@Component
public class ApprovalCompletedEventListener {

    private final ApprovalResultPort resultPort;

    public ApprovalCompletedEventListener(ApprovalResultPort resultPort) {
        this.resultPort = resultPort;
    }

    @EventListener
    public void handle(ApprovalCompletedEvent event) {
        resultPort.onApprovalCompleted(event.draftId(), event.approvalRequestId(), event.status(), event.actedBy(), event.comment());
    }
}
