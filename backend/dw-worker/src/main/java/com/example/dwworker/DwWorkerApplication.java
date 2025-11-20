package com.example.dwworker;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.dw.config.DwIngestionProperties;

@SpringBootApplication(scanBasePackages = {
        "com.example.dwworker",
        "com.example.dw",
        "com.example.batch",
        "com.example.file",
        "com.example.common"
})
@EntityScan({"com.example.dw", "com.example.file"})
@EnableJpaRepositories({
        "com.example.dw.domain",
        "com.example.dw.infrastructure.persistence",
        "com.example.file"
})
@EnableScheduling
@EnableConfigurationProperties(DwIngestionProperties.class)
public class DwWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DwWorkerApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
