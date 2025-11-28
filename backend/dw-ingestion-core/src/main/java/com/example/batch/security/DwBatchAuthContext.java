package com.example.batch.security;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;

/**
 * Provides a stable {@link AuthContext} for DW 배치 및 Quartz 작업.
 */
public final class DwBatchAuthContext {

    private static final AuthContext DW_SYSTEM_CONTEXT = AuthContext.of(
            "dw-batch",
            "ROOT",
            "DEFAULT",
            FeatureCode.HR_IMPORT,
            ActionCode.UPDATE,
            RowScope.ALL
    );

    private DwBatchAuthContext() {
    }

    public static AuthContext systemContext() {
        return DW_SYSTEM_CONTEXT;
    }
}
