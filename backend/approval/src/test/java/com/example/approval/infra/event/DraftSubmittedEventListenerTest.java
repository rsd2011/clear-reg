package com.example.approval.infra.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.approval.api.ApprovalFacade;
import com.example.approval.api.event.DraftSubmittedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

class DraftSubmittedEventListenerTest {

    @Test
    void listenerDelegatesToApprovalFacade() {
        ApprovalFacade facade = Mockito.mock(ApprovalFacade.class);
        DraftSubmittedEventListener listener = new DraftSubmittedEventListener(facade);

        DraftSubmittedEvent event = new DraftSubmittedEvent(UUID.randomUUID(), "T", "ORG", "user", "summary", List.of("G1"));
        listener.handle(event);

        verify(facade).requestApproval(any());
    }
}
