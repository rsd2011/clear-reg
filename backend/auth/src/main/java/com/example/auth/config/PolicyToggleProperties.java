package com.example.auth.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.auth.LoginType;

@ConfigurationProperties(prefix = "security.policy")
public class PolicyToggleProperties {

    private boolean passwordPolicyEnabled = true;
    private boolean passwordHistoryEnabled = true;
    private boolean accountLockEnabled = true;
    private List<LoginType> enabledLoginTypes = new ArrayList<>(EnumSet.allOf(LoginType.class));
    private long maxFileSizeBytes = 20 * 1024 * 1024; // 20MB
    private List<String> allowedFileExtensions = new ArrayList<>(List.of("pdf", "png", "jpg", "jpeg", "txt"));
    private boolean strictMimeValidation = true;
    private int fileRetentionDays = 365;

    public boolean isPasswordPolicyEnabled() {
        return passwordPolicyEnabled;
    }

    public void setPasswordPolicyEnabled(boolean passwordPolicyEnabled) {
        this.passwordPolicyEnabled = passwordPolicyEnabled;
    }

    public boolean isPasswordHistoryEnabled() {
        return passwordHistoryEnabled;
    }

    public void setPasswordHistoryEnabled(boolean passwordHistoryEnabled) {
        this.passwordHistoryEnabled = passwordHistoryEnabled;
    }

    public boolean isAccountLockEnabled() {
        return accountLockEnabled;
    }

    public void setAccountLockEnabled(boolean accountLockEnabled) {
        this.accountLockEnabled = accountLockEnabled;
    }

    public List<LoginType> getEnabledLoginTypes() {
        return enabledLoginTypes;
    }

    public void setEnabledLoginTypes(List<LoginType> enabledLoginTypes) {
        this.enabledLoginTypes = enabledLoginTypes;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedFileExtensions() {
        return allowedFileExtensions;
    }

    public void setAllowedFileExtensions(List<String> allowedFileExtensions) {
        this.allowedFileExtensions = allowedFileExtensions;
    }

    public boolean isStrictMimeValidation() {
        return strictMimeValidation;
    }

    public void setStrictMimeValidation(boolean strictMimeValidation) {
        this.strictMimeValidation = strictMimeValidation;
    }

    public int getFileRetentionDays() {
        return fileRetentionDays;
    }

    public void setFileRetentionDays(int fileRetentionDays) {
        this.fileRetentionDays = fileRetentionDays;
    }
}
