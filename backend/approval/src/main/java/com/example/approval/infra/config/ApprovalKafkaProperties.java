package com.example.approval.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "approval.kafka")
@Getter
@Setter
public class ApprovalKafkaProperties {
    private boolean enabled = false;
    private String draftSubmittedTopic = "draft-submitted";
    private String approvalCompletedTopic = "approval-completed";
}
