package com.example.admin.permission.context;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

import com.example.common.security.RowScope;
import com.example.common.security.RowScopeSpecifications;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationTreeService;

/**
 * ThreadLocal 기반 인증/인가 컨텍스트 홀더.
 *
 * <p>{@code @RequirePermission} AOP에서 설정하며, 현재 요청의 권한 정보를 제공한다.
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
   * 현재 AuthContext 기반으로 조직 범위 JPA Specification을 생성한다.
   *
   * <p>개발자가 Repository나 QueryDSL에서 RowScope 기반 행 필터링을 쉽게 적용하기 위한
   * 헬퍼 메서드이다. 자동 적용이 아닌 명시적 호출 방식으로, 서비스 흐름에 따라 개발자가
   * 적절한 시점에 사용한다.
   *
   * <p>사용 예시:
   * <pre>{@code
   * // Service에서 주입
   * private final DwOrganizationTreeService organizationTreeService;
   *
   * // Repository 호출 시
   * Specification<MyEntity> spec = AuthContextHolder.rowScopeSpec(
   *     "organizationCode", organizationTreeService);
   * repository.findAll(spec.and(otherConditions));
   * }</pre>
   *
   * @param organizationProperty    엔티티의 조직 코드 필드명 (예: "organizationCode")
   * @param organizationTreeService DW 조직 트리 서비스 (ORG 범위에서 하위 조직 조회용)
   * @param <T>                     대상 엔티티 타입
   * @return RowScope 기반 JPA Specification, AuthContext가 없으면 empty Optional
   */
  public static <T> Optional<Specification<T>> rowScopeSpec(
      String organizationProperty,
      DwOrganizationTreeService organizationTreeService) {
    return rowScopeSpec(organizationProperty, organizationTreeService, null);
  }

  /**
   * 현재 AuthContext 기반으로 조직 범위 JPA Specification을 생성한다 (CUSTOM 지원).
   *
   * @param organizationProperty    엔티티의 조직 코드 필드명
   * @param organizationTreeService DW 조직 트리 서비스
   * @param customSpecification     CUSTOM RowScope일 때 사용할 Specification (null 가능)
   * @param <T>                     대상 엔티티 타입
   * @return RowScope 기반 JPA Specification, AuthContext가 없으면 empty Optional
   */
  public static <T> Optional<Specification<T>> rowScopeSpec(
      String organizationProperty,
      DwOrganizationTreeService organizationTreeService,
      Specification<T> customSpecification) {

    return current().map(ctx -> {
      RowScope rowScope = ctx.rowScope();
      String orgCode = ctx.organizationCode();

      Collection<String> hierarchy = null;
      if (rowScope == RowScope.ORG && organizationTreeService != null) {
        hierarchy = organizationTreeService.snapshot()
            .descendantsIncluding(orgCode)
            .stream()
            .map(DwOrganizationNode::organizationCode)
            .toList();
      }

      return RowScopeSpecifications.organizationScoped(
          organizationProperty,
          rowScope,
          orgCode,
          hierarchy,
          customSpecification
      );
    });
  }

  /**
   * 현재 AuthContext의 RowScope를 반환한다.
   *
   * @return 현재 RowScope, AuthContext가 없으면 empty Optional
   */
  public static Optional<RowScope> currentRowScope() {
    return current().map(AuthContext::rowScope);
  }

  /**
   * 현재 AuthContext의 조직 코드를 반환한다.
   *
   * @return 현재 조직 코드, AuthContext가 없으면 empty Optional
   */
  public static Optional<String> currentOrganizationCode() {
    return current().map(AuthContext::organizationCode);
  }
}
