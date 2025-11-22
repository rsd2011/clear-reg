package com.example.server.readmodel;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.dw.application.readmodel.MenuReadModel;
import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;

@SpringBootTest(properties = {
        "readmodel.worker.enabled=true",
        "readmodel.organization.enabled=false",
        "readmodel.menu.enabled=false",
        "readmodel.permission-menu.enabled=false"
})
@ImportAutoConfiguration(RedisAutoConfiguration.class)
class ReadModelWorkerIntegrationTest {

    @Configuration
    static class TestConfig {
        @Bean
        ReadModelWorker readModelWorker(OrganizationReadModelPort orgPort,
                                        MenuReadModelPort menuPort,
                                        PermissionMenuReadModelPort permPort) {
            return new ReadModelWorker(orgPort, menuPort, permPort);
        }
    }

    @MockBean
    private OrganizationReadModelPort organizationReadModelPort;
    @MockBean
    private MenuReadModelPort menuReadModelPort;
    @MockBean
    private PermissionMenuReadModelPort permissionMenuReadModelPort;

    @Test
    void rebuildsAllWhenInvoked() {
        when(organizationReadModelPort.isEnabled()).thenReturn(true);
        when(menuReadModelPort.isEnabled()).thenReturn(true);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(true);

        ReadModelWorker worker = new ReadModelWorker(organizationReadModelPort, menuReadModelPort, permissionMenuReadModelPort);

        worker.rebuildOrganization();
        worker.rebuildMenu();
        worker.rebuildPermissionMenu("user1");

        verify(organizationReadModelPort).rebuild();
        verify(menuReadModelPort).rebuild();
        verify(permissionMenuReadModelPort).rebuild("user1");
    }

    @Test
    void loadsWhenEnabled() {
        when(organizationReadModelPort.isEnabled()).thenReturn(true);
        when(menuReadModelPort.isEnabled()).thenReturn(true);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(true);
        when(organizationReadModelPort.load()).thenReturn(Optional.of(new OrganizationTreeReadModel("v", java.time.OffsetDateTime.now(), java.util.List.of())));
        when(menuReadModelPort.load()).thenReturn(Optional.of(new MenuReadModel("v", java.time.OffsetDateTime.now(), java.util.List.of())));
        when(permissionMenuReadModelPort.load("u")).thenReturn(Optional.of(new PermissionMenuReadModel("v", java.time.OffsetDateTime.now(), java.util.List.of())));

        ReadModelWorker worker = new ReadModelWorker(organizationReadModelPort, menuReadModelPort, permissionMenuReadModelPort);

        worker.loadOrganization();
        worker.loadMenu();
        worker.loadPermissionMenu("u");

        verify(organizationReadModelPort).load();
        verify(menuReadModelPort).load();
        verify(permissionMenuReadModelPort).load("u");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("ReadModel 비활성화 시 rebuild를 건너뛴다")
    void skipWhenDisabled() {
        when(organizationReadModelPort.isEnabled()).thenReturn(false);
        when(menuReadModelPort.isEnabled()).thenReturn(false);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(false);

        ReadModelWorker worker = new ReadModelWorker(organizationReadModelPort, menuReadModelPort, permissionMenuReadModelPort);

        org.assertj.core.api.Assertions.assertThatCode(worker::rebuildOrganization).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(worker::rebuildMenu).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() -> worker.rebuildPermissionMenu("u")).doesNotThrowAnyException();
    }
}
