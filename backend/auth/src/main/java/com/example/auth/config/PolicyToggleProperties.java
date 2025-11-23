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
    private boolean auditEnabled = true;
    private boolean auditReasonRequired = true;
    private boolean auditSensitiveApiDefaultOn = true;
    private int auditRetentionDays = 730;
    private boolean auditStrictMode = true;
    private String auditRiskLevel = "MEDIUM";
    private List<String> auditSensitiveEndpoints = new ArrayList<>();
    private boolean auditMaskingEnabled = true;
    private java.util.List<String> auditUnmaskRoles = new java.util.ArrayList<>();
    private boolean auditPartitionEnabled = false;
    private String auditPartitionCron = "0 0 2 1 * *";
    private int auditPartitionPreloadMonths = 1;
    private boolean auditMonthlyReportEnabled = true;
    private String auditMonthlyReportCron = "0 0 4 1 * *";

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

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public boolean isAuditReasonRequired() {
        return auditReasonRequired;
    }

    public void setAuditReasonRequired(boolean auditReasonRequired) {
        this.auditReasonRequired = auditReasonRequired;
    }

    public boolean isAuditSensitiveApiDefaultOn() {
        return auditSensitiveApiDefaultOn;
    }

    public void setAuditSensitiveApiDefaultOn(boolean auditSensitiveApiDefaultOn) {
        this.auditSensitiveApiDefaultOn = auditSensitiveApiDefaultOn;
    }

    public int getAuditRetentionDays() {
        return auditRetentionDays;
    }

    public void setAuditRetentionDays(int auditRetentionDays) {
        this.auditRetentionDays = auditRetentionDays;
    }

    public boolean isAuditStrictMode() {
        return auditStrictMode;
    }

    public void setAuditStrictMode(boolean auditStrictMode) {
        this.auditStrictMode = auditStrictMode;
    }

    public String getAuditRiskLevel() {
        return auditRiskLevel;
    }

    public void setAuditRiskLevel(String auditRiskLevel) {
        this.auditRiskLevel = auditRiskLevel;
    }

    public List<String> getAuditSensitiveEndpoints() {
        return auditSensitiveEndpoints;
    }

    public void setAuditSensitiveEndpoints(List<String> auditSensitiveEndpoints) {
        this.auditSensitiveEndpoints = auditSensitiveEndpoints;
    }

    public boolean isAuditMaskingEnabled() {
        return auditMaskingEnabled;
    }

    public void setAuditMaskingEnabled(boolean auditMaskingEnabled) {
        this.auditMaskingEnabled = auditMaskingEnabled;
    }

    public java.util.List<String> getAuditUnmaskRoles() {
        return auditUnmaskRoles;
    }

    public void setAuditUnmaskRoles(java.util.List<String> auditUnmaskRoles) {
        this.auditUnmaskRoles = auditUnmaskRoles;
    }

    public boolean isAuditPartitionEnabled() {
        return auditPartitionEnabled;
    }

    public void setAuditPartitionEnabled(boolean auditPartitionEnabled) {
        this.auditPartitionEnabled = auditPartitionEnabled;
    }

    public String getAuditPartitionCron() {
        return auditPartitionCron;
    }

    public void setAuditPartitionCron(String auditPartitionCron) {
        this.auditPartitionCron = auditPartitionCron;
    }

    public int getAuditPartitionPreloadMonths() {
        return auditPartitionPreloadMonths;
    }

    public void setAuditPartitionPreloadMonths(int auditPartitionPreloadMonths) {
        this.auditPartitionPreloadMonths = auditPartitionPreloadMonths;
    }

    public boolean isAuditMonthlyReportEnabled() {
        return auditMonthlyReportEnabled;
    }

    public void setAuditMonthlyReportEnabled(boolean auditMonthlyReportEnabled) {
        this.auditMonthlyReportEnabled = auditMonthlyReportEnabled;
    }

    public String getAuditMonthlyReportCron() {
        return auditMonthlyReportCron;
    }

    public void setAuditMonthlyReportCron(String auditMonthlyReportCron) {
        this.auditMonthlyReportCron = auditMonthlyReportCron;
    }
}
