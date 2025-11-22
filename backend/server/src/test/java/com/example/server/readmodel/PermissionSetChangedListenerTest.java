package com.example.server.readmodel;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.auth.permission.event.PermissionSetChangedEvent;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;

class PermissionSetChangedListenerTest {

    private final OrganizationReadModelPort readModelPort = Mockito.mock(OrganizationReadModelPort.class);
    private final MenuReadModelPort menuReadModelPort = Mockito.mock(MenuReadModelPort.class);
    private final PermissionMenuReadModelPort permissionMenuReadModelPort = Mockito.mock(PermissionMenuReadModelPort.class);
    private final PermissionSetChangedListener listener = new PermissionSetChangedListener(readModelPort, menuReadModelPort, permissionMenuReadModelPort);

    @Test
    void triggersRebuildWhenEnabled() {
        when(readModelPort.isEnabled()).thenReturn(true);
        when(menuReadModelPort.isEnabled()).thenReturn(true);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(true);

        listener.onPermissionChanged(new PermissionSetChangedEvent("user-1"));

        verify(readModelPort).rebuild();
        verify(menuReadModelPort).rebuild();
        verify(permissionMenuReadModelPort).rebuild("user-1");
    }

    @Test
    void skipsWhenOrganizationReadModelDisabled() {
        when(readModelPort.isEnabled()).thenReturn(false);

        listener.onPermissionChanged(new PermissionSetChangedEvent("user-2"));

        Mockito.verifyNoInteractions(menuReadModelPort, permissionMenuReadModelPort);
        Mockito.verify(readModelPort, Mockito.never()).rebuild();
    }

    @Test
    void permissionMenuSkippedWhenPrincipalMissing() {
        when(readModelPort.isEnabled()).thenReturn(true);
        when(menuReadModelPort.isEnabled()).thenReturn(true);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(true);

        listener.onPermissionChanged(new PermissionSetChangedEvent(null));

        verify(readModelPort).rebuild();
        verify(menuReadModelPort).rebuild();
        Mockito.verify(permissionMenuReadModelPort, Mockito.never()).rebuild(Mockito.any());
    }

    @Test
    void skipsWhenOrganizationReadModelPortIsNull() {
        PermissionSetChangedListener listener = new PermissionSetChangedListener(null, menuReadModelPort, permissionMenuReadModelPort);

        listener.onPermissionChanged(new PermissionSetChangedEvent("user3"));

        Mockito.verifyNoInteractions(menuReadModelPort, permissionMenuReadModelPort);
    }

    @Test
    void menuReadModelDisabledDoesNotRebuild() {
        when(readModelPort.isEnabled()).thenReturn(true);
        when(menuReadModelPort.isEnabled()).thenReturn(false);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(false);

        listener.onPermissionChanged(new PermissionSetChangedEvent("user4"));

        verify(readModelPort).rebuild();
        Mockito.verify(menuReadModelPort, Mockito.never()).rebuild();
        Mockito.verify(permissionMenuReadModelPort, Mockito.never()).rebuild(Mockito.any());
    }
}
