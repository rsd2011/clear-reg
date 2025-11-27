package com.example.admin.permission.audit;

import com.example.admin.permission.context.AuthContext;

public interface PermissionAuditLogger {

  void onAccessGranted(AuthContext context);

  void onAccessDenied(AuthContext context, Throwable throwable);
}
