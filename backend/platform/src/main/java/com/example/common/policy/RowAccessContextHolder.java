package com.example.common.policy;

/**
 * ThreadLocal에 RowAccessMatch를 저장하여 서비스/리포지토리 계층에서 사용하도록 한다.
 */
public final class RowAccessContextHolder {

    private static final ThreadLocal<RowAccessMatch> HOLDER = new ThreadLocal<>();

    private RowAccessContextHolder() {}

    public static void set(RowAccessMatch match) {
        HOLDER.set(match);
    }

    public static RowAccessMatch get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
