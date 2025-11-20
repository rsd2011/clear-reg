package com.example.draft.application.audit;

import java.util.Optional;

public final class AuditRequestContextHolder {

    private static final ThreadLocal<AuditRequestContext> CURRENT = new ThreadLocal<>();

    private AuditRequestContextHolder() {
    }

    public static void set(AuditRequestContext context) {
        CURRENT.set(context);
    }

    public static Optional<AuditRequestContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
