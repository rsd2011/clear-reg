package com.example.admin.permission.context;

import java.util.Optional;

/**
 * ThreadLocal 기반 인증/인가 컨텍스트 홀더.
 *
 * <p>{@code @RequirePermission} AOP에서 설정하며, 현재 요청의 권한 정보를 제공한다.
 *
 * <p>RowScope 관련 기능은 RowAccessPolicy로 이관되었습니다.
 */
public final class AuthContextHolder {

  private static final ThreadLocal<AuthContext> CONTEXT = new ThreadLocal<>();

  private AuthContextHolder() {}

  public static void set(AuthContext context) {
    CONTEXT.set(context);
  }

  public static Optional<AuthContext> current() {
    return Optional.ofNullable(CONTEXT.get());
  }

  public static void clear() {
    CONTEXT.remove();
  }

  /**
   * 현재 AuthContext의 조직 코드를 반환한다.
   *
   * @return 현재 조직 코드, AuthContext가 없으면 empty Optional
   */
  public static Optional<String> currentOrganizationCode() {
    return current().map(AuthContext::organizationCode);
  }

  /**
   * 현재 AuthContext의 사용자명을 반환한다.
   *
   * @return 현재 사용자명, AuthContext가 없으면 empty Optional
   */
  public static Optional<String> currentUsername() {
    return current().map(AuthContext::username);
  }

  /**
   * 현재 AuthContext의 권한 그룹 코드를 반환한다.
   *
   * @return 현재 권한 그룹 코드, AuthContext가 없으면 empty Optional
   */
  public static Optional<String> currentPermissionGroupCode() {
    return current().map(AuthContext::permissionGroupCode);
  }
}
