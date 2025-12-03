package com.example.server.user;

import com.example.admin.user.domain.UserAccount;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeSpecifications;
import com.example.server.user.dto.UserSearchCriteria;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * 사용자 검색 Specification.
 */
public final class UserAccountSpecifications {

  private UserAccountSpecifications() {
  }

  /**
   * 검색 조건과 RowScope를 적용한 Specification을 생성합니다.
   *
   * @param criteria 검색 조건
   * @param rowScope 조직 범위
   * @param organizationCode 현재 사용자 조직 코드
   * @param organizationHierarchy 조직 계층 (ORG scope인 경우)
   * @return Specification
   */
  public static Specification<UserAccount> withCriteriaAndRowScope(
      UserSearchCriteria criteria,
      RowScope rowScope,
      String organizationCode,
      Collection<String> organizationHierarchy) {

    Specification<UserAccount> spec = Specification.where(null);

    // 검색 조건 적용
    if (criteria != null) {
      if (StringUtils.hasText(criteria.username())) {
        spec = spec.and(usernameLike(criteria.username()));
      }
      if (StringUtils.hasText(criteria.email())) {
        spec = spec.and(emailLike(criteria.email()));
      }
      if (StringUtils.hasText(criteria.organizationCode())) {
        spec = spec.and(organizationCodeEquals(criteria.organizationCode()));
      }
      if (StringUtils.hasText(criteria.permissionGroupCode())) {
        spec = spec.and(permissionGroupCodeEquals(criteria.permissionGroupCode()));
      }
      if (criteria.active() != null) {
        spec = spec.and(activeEquals(criteria.active()));
      }
    }

    // RowScope 적용
    Specification<UserAccount> rowScopeSpec = RowScopeSpecifications.organizationScoped(
        "organizationCode",
        rowScope,
        organizationCode,
        organizationHierarchy
    );

    return spec.and(rowScopeSpec);
  }

  private static Specification<UserAccount> usernameLike(String username) {
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
  }

  private static Specification<UserAccount> emailLike(String email) {
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
  }

  private static Specification<UserAccount> organizationCodeEquals(String organizationCode) {
    return (root, query, cb) ->
        cb.equal(root.get("organizationCode"), organizationCode);
  }

  private static Specification<UserAccount> permissionGroupCodeEquals(String permissionGroupCode) {
    return (root, query, cb) ->
        cb.equal(root.get("permissionGroupCode"), permissionGroupCode);
  }

  private static Specification<UserAccount> activeEquals(Boolean active) {
    return (root, query, cb) ->
        cb.equal(root.get("active"), active);
  }
}
