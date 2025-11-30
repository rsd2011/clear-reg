package com.example.admin.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * AuthContextHolder 테스트.
 *
 * <p>RowScope 관련 메서드는 RowAccessPolicy로 이관되어 제거되었습니다.
 */
@DisplayName("AuthContextHolder 테스트")
class AuthContextHolderTest {

  @AfterEach
  void cleanup() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("Given AuthContext When set 호출 Then clear 전까지 조회할 수 있다")
  void givenContext_whenSet_thenAvailableUntilCleared() {
    AuthContext context =
        AuthContext.of(
            "tester",
            "ORG",
            "AUDIT",
            FeatureCode.ORGANIZATION,
            ActionCode.READ,
            List.of("ORG_GROUP1"));
    AuthContextHolder.set(context);

    assertThat(AuthContextHolder.current()).contains(context);
    AuthContextHolder.clear();
    assertThat(AuthContextHolder.current()).isEmpty();
  }

  @Nested
  @DisplayName("currentOrganizationCode 메서드")
  class CurrentOrganizationCodeTest {

    @Test
    @DisplayName("Given AuthContext 설정됨 When currentOrganizationCode Then 조직코드 반환")
    void givenContext_whenCurrentOrganizationCode_thenReturnsOrgCode() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      assertThat(AuthContextHolder.currentOrganizationCode()).contains("ORG001");
    }

    @Test
    @DisplayName("Given AuthContext 미설정 When currentOrganizationCode Then empty")
    void givenNoContext_whenCurrentOrganizationCode_thenEmpty() {
      assertThat(AuthContextHolder.currentOrganizationCode()).isEmpty();
    }
  }

  @Nested
  @DisplayName("currentUsername 메서드")
  class CurrentUsernameTest {

    @Test
    @DisplayName("Given AuthContext 설정됨 When currentUsername Then 사용자명 반환")
    void givenContext_whenCurrentUsername_thenReturnsUsername() {
      AuthContext context = AuthContext.of(
          "test-user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      assertThat(AuthContextHolder.currentUsername()).contains("test-user");
    }

    @Test
    @DisplayName("Given AuthContext 미설정 When currentUsername Then empty")
    void givenNoContext_whenCurrentUsername_thenEmpty() {
      assertThat(AuthContextHolder.currentUsername()).isEmpty();
    }
  }

  @Nested
  @DisplayName("currentPermissionGroupCode 메서드")
  class CurrentPermissionGroupCodeTest {

    @Test
    @DisplayName("Given AuthContext 설정됨 When currentPermissionGroupCode Then 권한그룹코드 반환")
    void givenContext_whenCurrentPermissionGroupCode_thenReturnsGroupCode() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "ADMIN_GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      assertThat(AuthContextHolder.currentPermissionGroupCode()).contains("ADMIN_GROUP");
    }

    @Test
    @DisplayName("Given AuthContext 미설정 When currentPermissionGroupCode Then empty")
    void givenNoContext_whenCurrentPermissionGroupCode_thenEmpty() {
      assertThat(AuthContextHolder.currentPermissionGroupCode()).isEmpty();
    }
  }

  @Nested
  @DisplayName("orgGroupCodes 관련 테스트")
  class OrgGroupCodesTest {

    @Test
    @DisplayName("Given AuthContext with orgGroupCodes When set Then 조회할 수 있다")
    void givenContextWithOrgGroupCodes_whenSet_thenCanRetrieve() {
      List<String> orgGroupCodes = List.of("GROUP1", "GROUP2");
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, orgGroupCodes);
      AuthContextHolder.set(context);

      assertThat(AuthContextHolder.current())
          .isPresent()
          .hasValueSatisfying(ctx -> assertThat(ctx.orgGroupCodes()).isEqualTo(orgGroupCodes));
    }

    @Test
    @DisplayName("Given AuthContext with empty orgGroupCodes When set Then 빈 리스트 조회")
    void givenContextWithEmptyOrgGroupCodes_whenSet_thenEmptyList() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, List.of());
      AuthContextHolder.set(context);

      assertThat(AuthContextHolder.current())
          .isPresent()
          .hasValueSatisfying(ctx -> assertThat(ctx.orgGroupCodes()).isEmpty());
    }
  }
}
