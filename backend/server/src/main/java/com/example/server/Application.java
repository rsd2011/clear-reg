package com.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.auth.security.JwtProperties;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.config.SessionPolicyProperties;
import com.example.dw.config.DwIngestionProperties;
import com.example.server.readmodel.MenuReadModelProperties;
import com.example.server.readmodel.OrganizationReadModelProperties;
import com.example.server.readmodel.PermissionMenuReadModelProperties;

@SpringBootApplication(scanBasePackages = {
        "com.example.server",
        "com.example.auth",
        "com.example.admin",
        "com.example.common",
        "com.example.dw",
        "com.example.draft",
        "com.example.file",
        "com.example.platform"
})
@EnableConfigurationProperties({JwtProperties.class, AuthPolicyProperties.class, SessionPolicyProperties.class,
        DwIngestionProperties.class, OrganizationReadModelProperties.class, MenuReadModelProperties.class,
        PermissionMenuReadModelProperties.class})
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
