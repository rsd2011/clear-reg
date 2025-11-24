package com.example.common.policy;

/**
 * Provider abstraction that exposes the current policy toggle snapshot to consumers.
 */
public interface PolicySettingsProvider {

    /**
     * Returns the currently active toggle configuration loaded from the policy service.
     *
     * @return immutable snapshot
     */
    PolicyToggleSettings currentSettings();

    /**
     * Optional audit partition tuning settings (HOT/COLD, cron, preload).
     * Default implementation returns null to preserve binary compatibility.
     */
    default AuditPartitionSettings partitionSettings() {
        return null;
    }

    /**
     * Optional per-job schedule settings when 중앙 스케줄러를 사용할 때 참고.
     * 기본 구현은 null 반환(미정의).
     */
    default com.example.common.schedule.BatchJobSchedule batchJobSchedule(com.example.common.schedule.BatchJobCode code) {
        return null;
    }
}
