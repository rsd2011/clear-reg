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
}
