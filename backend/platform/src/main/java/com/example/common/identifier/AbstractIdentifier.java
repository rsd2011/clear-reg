package com.example.common.identifier;

/**
 * 공통 식별자 기본 기능: 검증, 마스킹, 원문 접근.
 */
abstract class AbstractIdentifier {

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 64;

    protected static String normalizeAndValidate(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(name + " length must be between " + MIN_LENGTH + " and " + MAX_LENGTH);
        }
        if (!trimmed.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(name + " may contain only A-Z, a-z, 0-9, _ or -");
        }
        return trimmed;
    }

    protected static String mask(String raw) {
        int len = raw.length();
        int visible = Math.min(4, len);
        String suffix = raw.substring(len - visible);
        return "*".repeat(Math.max(0, len - visible)) + suffix;
    }
}
