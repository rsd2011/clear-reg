package com.example.common.ulid;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link ValidUlid} 어노테이션에 대한 검증기.
 *
 * <p>문자열이 유효한 ULID(26자) 또는 UUID(36자) 형식인지 검증합니다.</p>
 *
 * @see ValidUlid
 * @see UlidUtils
 */
public class UlidValidator implements ConstraintValidator<ValidUlid, String> {

    private boolean ulidOnly;

    @Override
    public void initialize(ValidUlid constraintAnnotation) {
        this.ulidOnly = constraintAnnotation.ulidOnly();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null은 @NotNull로 별도 검증
        if (value == null || value.isBlank()) {
            return true;
        }

        if (ulidOnly) {
            return UlidUtils.isValidUlid(value);
        }

        return UlidUtils.isValidId(value);
    }
}
