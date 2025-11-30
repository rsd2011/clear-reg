package com.example.admin.permission.context;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.common.security.CurrentUser;
import com.example.common.security.CurrentUserProvider;
import com.example.common.security.RowScope;

/**
 * AuthContext를 CurrentUser로 변환하는 Provider.
 *
 * <p>RowScope는 RowAccessPolicy로 이관되었으므로, 기본값 ALL을 사용합니다.
 * 실제 행 수준 접근 제어는 RowAccessPolicy에서 수행합니다.
 */
@Component
public class AuthCurrentUserProvider implements CurrentUserProvider {

  @Override
  public Optional<CurrentUser> current() {
    return AuthContextHolder.current().map(this::toCurrentUser);
  }

  private CurrentUser toCurrentUser(AuthContext context) {
    return new CurrentUser(
        context.username(),
        context.organizationCode(),
        context.permissionGroupCode(),
        context.feature() != null ? context.feature().name() : null,
        context.action() != null ? context.action().name() : null,
        RowScope.ALL, // RowScope는 RowAccessPolicy로 이관됨
        context.orgGroupCodes()
    );
  }
}
