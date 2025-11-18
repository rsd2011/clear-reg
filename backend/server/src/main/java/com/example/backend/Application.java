package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.auth.security.JwtProperties;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.config.SessionPolicyProperties;
import com.example.hr.config.HrIngestionProperties;

@SpringBootApplication(scanBasePackages = "com.example")
@EnableConfigurationProperties({JwtProperties.class, AuthPolicyProperties.class, SessionPolicyProperties.class,
        HrIngestionProperties.class})
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
