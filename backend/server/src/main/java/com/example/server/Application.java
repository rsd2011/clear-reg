package com.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.auth.security.JwtProperties;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.config.SessionPolicyProperties;
import com.example.dw.config.DwIngestionProperties;

@SpringBootApplication(scanBasePackages = {
        "com.example.server",
        "com.example.auth",
        "com.example.common",
        "com.example.dw",
        "com.example.draft",
        "com.example.file",
        "com.example.policy",
        "com.example.platform"
})
@EnableConfigurationProperties({JwtProperties.class, AuthPolicyProperties.class, SessionPolicyProperties.class,
        DwIngestionProperties.class})
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
