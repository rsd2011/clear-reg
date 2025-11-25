package com.example.approval.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "approval.kafka")
public class ApprovalKafkaProperties {
    private boolean enabled = false;
    private String draftSubmittedTopic = "draft-submitted";
    private String approvalCompletedTopic = "approval-completed";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDraftSubmittedTopic() {
        return draftSubmittedTopic;
    }

    public void setDraftSubmittedTopic(String draftSubmittedTopic) {
        this.draftSubmittedTopic = draftSubmittedTopic;
    }

    public String getApprovalCompletedTopic() {
        return approvalCompletedTopic;
    }

    public void setApprovalCompletedTopic(String approvalCompletedTopic) {
        this.approvalCompletedTopic = approvalCompletedTopic;
    }
}
