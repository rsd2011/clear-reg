package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;

class ReadModelWorkerSkipErrorTest {

    OrganizationReadModelPort orgPort = Mockito.mock(OrganizationReadModelPort.class);
    MenuReadModelPort menuPort = Mockito.mock(MenuReadModelPort.class);
    PermissionMenuReadModelPort permPort = Mockito.mock(PermissionMenuReadModelPort.class);

    @Test
    @DisplayName("rebuild 중 예외가 발생하면 그대로 전파한다")
    void rebuildPropagatesError() {
        when(orgPort.isEnabled()).thenReturn(true);
        when(menuPort.isEnabled()).thenReturn(true);
        when(permPort.isEnabled()).thenReturn(true);
        Mockito.doThrow(new IllegalStateException("boom")).when(orgPort).rebuild();

        ReadModelWorker worker = new ReadModelWorker(orgPort, menuPort, permPort);

        assertThatThrownBy(worker::rebuildOrganization)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
    }

    @Test
    @DisplayName("load 시 비활성화된 readmodel은 호출하지 않는다")
    void loadSkipsWhenDisabled() {
        when(orgPort.isEnabled()).thenReturn(false);
        ReadModelWorker worker = new ReadModelWorker(orgPort, menuPort, permPort);

        Optional<?> result = worker.loadOrganization();

        Mockito.verify(orgPort, Mockito.never()).load();
        org.assertj.core.api.Assertions.assertThat(result).isEmpty();
    }
}
