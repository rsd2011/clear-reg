package com.example.server.readmodel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;
import com.example.dw.application.readmodel.OrganizationReadModelPort;

@Configuration
@ConditionalOnProperty(prefix = "readmodel.worker", name = "enabled", havingValue = "true")
public class ReadModelWorkerConfiguration {

    @Bean
    public ReadModelWorker readModelWorker(OrganizationReadModelPort organizationReadModelPort,
                                           MenuReadModelPort menuReadModelPort,
                                           PermissionMenuReadModelPort permissionMenuReadModelPort) {
        return new ReadModelWorker(organizationReadModelPort, menuReadModelPort, permissionMenuReadModelPort);
    }
}
