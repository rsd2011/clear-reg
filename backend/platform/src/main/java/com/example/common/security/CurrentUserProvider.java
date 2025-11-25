package com.example.common.security;

import java.util.Optional;

/**
 * 현재 사용자 컨텍스트를 조회하는 포트.
 */
@FunctionalInterface
public interface CurrentUserProvider {
    Optional<CurrentUser> current();
}
