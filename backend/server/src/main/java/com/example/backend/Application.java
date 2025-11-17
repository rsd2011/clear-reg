package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.auth.security.JwtProperties;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.config.SessionPolicyProperties;

@SpringBootApplication(scanBasePackages = "com.example")
@EnableConfigurationProperties({JwtProperties.class, AuthPolicyProperties.class, SessionPolicyProperties.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
