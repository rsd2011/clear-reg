package com.example.server.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.admin.user.domain.UserAccount;
import com.example.common.security.RowScope;
import com.example.server.user.dto.UserSearchCriteria;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/**
 * UserAccountSpecifications 테스트.
 *
 * <p>Specification이 null이 아니고 정상적으로 생성되는지 검증합니다.
 * 실제 쿼리 실행은 통합 테스트에서 검증합니다.
 */
@DisplayName("UserAccountSpecifications 테스트")
class UserAccountSpecificationsTest {

  @Test
  @DisplayName("Given utility class When 리플렉션으로 생성자 호출 Then private 생성자 확인")
  void givenUtilityClass_whenReflection_thenPrivateConstructor() throws Exception {
    Constructor<UserAccountSpecifications> constructor =
        UserAccountSpecifications.class.getDeclaredConstructor();

    assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();

    // private 생성자를 호출하여 커버리지 확보
    constructor.setAccessible(true);
    UserAccountSpecifications instance = constructor.newInstance();
    assertThat(instance).isNotNull();
  }

  @Nested
  @DisplayName("검색 조건 적용")
  class SearchCriteriaTest {

    @Test
    @DisplayName("Given null 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenNullCriteria_whenBuildSpec_thenSpecIsNotNull() {
      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          null,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given username 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenUsernameCriteria_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          "testuser", null, null, null, null);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given email 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenEmailCriteria_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          null, "test@example.com", null, null, null);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given organizationCode 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenOrgCodeCriteria_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          null, null, "ORG-001", null, null);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given permissionGroupCode 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenPermissionGroupCriteria_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          null, null, null, "ADMIN_GROUP", null);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given active true 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenActiveTrueCriteria_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          null, null, null, null, true);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given active false 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenActiveFalseCriteria_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          null, null, null, null, false);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given 복합 검색 조건 When Specification 생성 Then Specification이 생성된다")
    void givenMultipleCriteria_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          "testuser", "test@example.com", "ORG-001", "ADMIN_GROUP", true);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }
  }

  @Nested
  @DisplayName("RowScope 적용")
  class RowScopeTest {

    @Test
    @DisplayName("Given ALL scope When Specification 생성 Then Specification이 생성된다")
    void givenAllScope_whenBuildSpec_thenSpecIsNotNull() {
      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          null,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given ORG scope When Specification 생성 Then Specification이 생성된다")
    void givenOrgScope_whenBuildSpec_thenSpecIsNotNull() {
      Collection<String> hierarchy = Set.of("ORG-001", "ORG-001-A", "ORG-001-B");

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          null,
          RowScope.ORG,
          "ORG-001",
          hierarchy
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given OWN scope When Specification 생성 Then Specification이 생성된다")
    void givenOwnScope_whenBuildSpec_thenSpecIsNotNull() {
      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          null,
          RowScope.OWN,
          "ORG-001",
          List.of("ORG-001")
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given CUSTOM scope When Specification 생성 Then customSpecification 필요로 예외 발생")
    void givenCustomScope_whenBuildSpec_thenThrowsException() {
      Collection<String> customOrgs = Set.of("ORG-002", "ORG-003");

      // CUSTOM scope는 customSpecification이 필요하므로 예외 발생
      assertThatThrownBy(() -> UserAccountSpecifications.withCriteriaAndRowScope(
          null,
          RowScope.CUSTOM,
          "ORG-001",
          customOrgs
      )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("customSpecification is required");
    }
  }

  @Nested
  @DisplayName("빈 문자열 처리")
  class EmptyStringHandlingTest {

    @Test
    @DisplayName("Given 빈 username When Specification 생성 Then Specification이 생성된다")
    void givenEmptyUsername_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          "", null, null, null, null);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }

    @Test
    @DisplayName("Given 공백 email When Specification 생성 Then Specification이 생성된다")
    void givenWhitespaceEmail_whenBuildSpec_thenSpecIsNotNull() {
      UserSearchCriteria criteria = new UserSearchCriteria(
          null, "   ", null, null, null);

      Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
          criteria,
          RowScope.ALL,
          "ORG-001",
          List.of()
      );

      assertThat(spec).isNotNull();
    }
  }
}
