package com.example.server.readmodel;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.auth.permission.event.PermissionSetChangedEvent;
import com.example.dw.application.readmodel.OrganizationReadModelPort;

class PermissionSetChangedListenerTest {

    private final OrganizationReadModelPort readModelPort = Mockito.mock(OrganizationReadModelPort.class);
    private final PermissionSetChangedListener listener = new PermissionSetChangedListener(readModelPort);

    @Test
    void triggersRebuildWhenEnabled() {
        when(readModelPort.isEnabled()).thenReturn(true);

        listener.onPermissionChanged(new PermissionSetChangedEvent("user-1"));

        verify(readModelPort).rebuild();
    }
}
