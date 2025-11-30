package com.example.batch.security;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.context.AuthContext;

import java.util.List;

/**
 * Provides a stable {@link AuthContext} for DW 배치 및 Quartz 작업.
 *
 * <p>RowScope는 RowAccessPolicy로 이관되었으므로 더 이상 AuthContext에 포함되지 않습니다.
 */
public final class DwBatchAuthContext {

    private static final AuthContext DW_SYSTEM_CONTEXT = AuthContext.of(
            "dw-batch",
            "ROOT",
            "DEFAULT",
            FeatureCode.HR_IMPORT,
            ActionCode.UPDATE,
            List.of() // orgGroupCodes - 배치 작업에서는 빈 리스트 사용
    );

    private DwBatchAuthContext() {
    }

    public static AuthContext systemContext() {
        return DW_SYSTEM_CONTEXT;
    }
}
