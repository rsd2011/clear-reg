package com.example.common.codegroup.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 코드 그룹 관리 대상 Enum을 표시하는 어노테이션.
 *
 * <p>이 어노테이션이 적용된 Enum 클래스는 코드 그룹 조회 및 관리 대상이 됩니다.</p>
 *
 * <pre>{@code
 * @ManagedCode(displayName = "결재 상태", description = "결재 프로세스의 상태 코드")
 * public enum ApprovalStatus {
 *     @CodeValue(label = "대기", order = 1)
 *     PENDING,
 *     @CodeValue(label = "승인", order = 2)
 *     APPROVED,
 *     @CodeValue(label = "반려", order = 3)
 *     REJECTED
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManagedCode {

    /**
     * 코드 그룹의 표시명.
     * 관리자 UI에서 사용됩니다.
     */
    String displayName() default "";

    /**
     * 코드 그룹의 설명.
     */
    String description() default "";

    /**
     * 코드 그룹의 분류.
     * 예: "system", "business", "approval"
     */
    String group() default "";

    /**
     * 관리자 UI에서의 표시 순서.
     */
    int displayOrder() default 0;

    /**
     * 관리자 UI에서 숨길지 여부.
     * true이면 코드 그룹 목록에서 제외됩니다.
     */
    boolean hidden() default false;
}
