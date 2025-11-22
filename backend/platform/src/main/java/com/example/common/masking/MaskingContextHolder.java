package com.example.common.masking;

/**
 * 요청 스코프 마스킹 컨텍스트 저장용 ThreadLocal.
 * 필터/인터셉터에서 subjectType 설정 후 DTO 변환 시 활용.
 */
public final class MaskingContextHolder {
    private static final ThreadLocal<MaskingTarget> CTX = new ThreadLocal<>();

    private MaskingContextHolder() {}

    public static void set(MaskingTarget target) {
        CTX.set(target);
    }

    public static MaskingTarget get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}
