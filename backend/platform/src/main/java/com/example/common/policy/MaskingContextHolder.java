package com.example.common.policy;

/**
 * ThreadLocal에 MaskingMatch를 저장하여 서비스/리포지토리 계층에서 사용하도록 한다.
 */
public final class MaskingContextHolder {

    private static final ThreadLocal<MaskingMatch> HOLDER = new ThreadLocal<>();

    private MaskingContextHolder() {}

    public static void set(MaskingMatch match) {
        HOLDER.set(match);
    }

    public static MaskingMatch get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
