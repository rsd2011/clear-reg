package com.example.common.cache;

/**
 * Canonical cache name constants shared across modules for consistent eviction.
 */
public final class CacheNames {

    /** Cache for resolved {@code UserDetails} objects. */
    public static final String USER_DETAILS = "userDetails";
    /** Cache for greeting responses used in health checks. */
    public static final String GREETINGS = "greetings";
    /** Cache for organization policy snapshots. */
    public static final String ORGANIZATION_POLICIES = "organizationPolicies";
    /** Cache for user account metadata pulled from auth services. */
    public static final String USER_ACCOUNTS = "userAccounts";
    /** Cache for latest DW ingestion batch summary. */
    public static final String LATEST_DW_BATCH = "latestDwBatch";
    /** Cache for row-scope permissions keyed by organization. */
    public static final String ORGANIZATION_ROW_SCOPE = "organizationRowScope";
    /** Cache for frequently accessed DW employee snapshots. */
    public static final String DW_EMPLOYEES = "dwEmployees";
    /** Cache for materialized DW organization tree structures. */
    public static final String DW_ORG_TREE = "dwOrganizationTree";
    /** Cache for DW provided common code lookups. */
    public static final String DW_COMMON_CODES = "dwCommonCodes";
    /** Cache for system-managed common code lookups. */
    public static final String SYSTEM_COMMON_CODES = "systemCommonCodes";
    /** Cache for aggregated common code responses served to clients. */
    public static final String COMMON_CODE_AGGREGATES = "commonCodeAggregates";

    private CacheNames() {
    }
}
