package com.example.common.policy;

/**
 * ThreadLocal에 DataPolicyMatch를 저장하여 서비스/리포지토리 계층에서 사용하도록 한다.
 */
public final class DataPolicyContextHolder {

    private static final ThreadLocal<DataPolicyMatch> HOLDER = new ThreadLocal<>();

    private DataPolicyContextHolder() {}

    public static void set(DataPolicyMatch match) {
        HOLDER.set(match);
    }

    public static DataPolicyMatch get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
