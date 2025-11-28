package com.example.common.ulid;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * ULID 또는 UUID 형식 문자열 검증 어노테이션.
 *
 * <p>Bean Validation을 통해 문자열이 유효한 ULID(26자) 또는 UUID(36자) 형식인지 검증합니다.</p>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * public record CreateDraftRequest(
 *     @ValidUlid String categoryId,
 *     String title
 * ) {}
 * }</pre>
 *
 * @see UlidValidator
 * @see UlidUtils
 */
@Documented
@Constraint(validatedBy = UlidValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUlid {

    /**
     * 검증 실패 시 오류 메시지.
     */
    String message() default "Invalid ID format. Expected ULID (26 chars) or UUID (36 chars).";

    /**
     * 검증 그룹.
     */
    Class<?>[] groups() default {};

    /**
     * 페이로드.
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * ULID 형식만 허용할지 여부 (기본: false, UUID도 허용).
     */
    boolean ulidOnly() default false;
}
