package com.example.dwgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example")
public class DwGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DwGatewayApplication.class, args);
    }
}
