package com.example.common.security;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks repositories that must enforce RowScope-aware filtering.
 *
 * @deprecated 이 마커 어노테이션은 더 이상 사용되지 않습니다.
 *     RowScope 기반 필터링은 {@link com.example.admin.permission.context.AuthContextHolder#rowScopeSpec}
 *     헬퍼 메서드를 통해 개발자가 명시적으로 적용합니다.
 *     자동 적용 대신 서비스 흐름에 따라 개발자가 적절한 시점에 Specification을 가져와 사용합니다.
 */
@Deprecated(since = "2024.1", forRemoval = true)
@Retention(RUNTIME)
@Target(TYPE)
public @interface RequiresRowScope {
}
