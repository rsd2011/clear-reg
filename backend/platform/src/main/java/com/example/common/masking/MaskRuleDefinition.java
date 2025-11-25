package com.example.common.masking;

/**
 * 마스킹 규칙의 최소 계약. JPA/권한 모듈에 의존하지 않는다.
 */
public record MaskRuleDefinition(String tag,
                                 String maskWith,
                                 String requiredActionCode,
                                 boolean audit) {

    public MaskRuleDefinition {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be blank");
        }
        maskWith = (maskWith == null || maskWith.isBlank()) ? "***" : maskWith;
        requiredActionCode = (requiredActionCode == null || requiredActionCode.isBlank())
                ? "UNMASK"
                : requiredActionCode;
    }
}
