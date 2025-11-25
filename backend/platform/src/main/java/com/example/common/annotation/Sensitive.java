package com.example.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 민감 정보 필드에 태그를 부여해 마스킹 규칙을 적용하기 위한 공통 애노테이션.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensitive {

    /**
     * 데이터 정책/마스킹 룰에서 사용할 태그.
     */
    String value();
}
