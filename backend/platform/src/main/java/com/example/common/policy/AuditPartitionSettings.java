package com.example.common.policy;

/**
 * Optional audit partition tuning settings (HOT/COLD tablespace, retention windows).
 * Kept separate to avoid breaking existing PolicyToggleSettings call sites.
 */
public record AuditPartitionSettings(
        boolean enabled,
        String cron,
        int preloadMonths,
        String tablespaceHot,
        String tablespaceCold,
        int hotMonths,
        int coldMonths) {

    public AuditPartitionSettings {
        if (cron == null || cron.isBlank()) {
            cron = "0 0 2 1 * *";
        }
        if (preloadMonths < 0) {
            preloadMonths = 0;
        }
        if (hotMonths <= 0) {
            hotMonths = 6;
        }
        if (coldMonths <= 0) {
            coldMonths = 60;
        }
        tablespaceHot = tablespaceHot == null ? "" : tablespaceHot;
        tablespaceCold = tablespaceCold == null ? "" : tablespaceCold;
    }
}
