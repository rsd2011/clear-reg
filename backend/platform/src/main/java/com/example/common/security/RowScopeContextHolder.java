package com.example.common.security;

/**
 * ThreadLocal 기반 RowScope 컨텍스트 저장소.
 * 컨트롤러/필터에서 설정 후 리포지토리 계층에서 재사용한다.
 */
public final class RowScopeContextHolder {

    private static final ThreadLocal<RowScopeContext> CONTEXT = new ThreadLocal<>();

    private RowScopeContextHolder() {
    }

    public static void set(RowScopeContext context) {
        CONTEXT.set(context);
    }

    public static RowScopeContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
