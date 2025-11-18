package com.example.auth.permission.audit;

import com.example.auth.permission.context.AuthContext;

public interface PermissionAuditLogger {

    void onAccessGranted(AuthContext context);

    void onAccessDenied(AuthContext context, Throwable throwable);
}
