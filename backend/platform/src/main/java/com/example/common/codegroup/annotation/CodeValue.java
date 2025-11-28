package com.example.common.codegroup.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 코드 항목(Enum 상수)의 메타데이터를 정의하는 어노테이션.
 *
 * <p>각 Enum 상수에 라벨, 설명, 표시 순서 등의 추가 정보를 부여합니다.</p>
 *
 * <pre>{@code
 * public enum ApprovalStatus {
 *     @CodeValue(label = "대기 중", description = "승인 대기 상태", order = 1)
 *     PENDING,
 *     @CodeValue(label = "승인됨", description = "승인 완료", order = 2)
 *     APPROVED,
 *     @CodeValue(label = "반려됨", description = "승인 거부", order = 3, deprecated = true)
 *     REJECTED
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeValue {

    /**
     * 코드 항목의 표시 라벨.
     * 비어있으면 Enum name()을 사용합니다.
     */
    String label() default "";

    /**
     * 코드 항목의 설명.
     */
    String description() default "";

    /**
     * 표시 순서.
     * 기본값 0이면 Enum 선언 순서를 따릅니다.
     */
    int order() default 0;

    /**
     * 더 이상 사용하지 않는 코드인지 여부.
     * true이면 UI에서 비활성 상태로 표시될 수 있습니다.
     */
    boolean deprecated() default false;
}
