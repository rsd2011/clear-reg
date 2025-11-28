package com.example.admin.permission.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

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
            RowScope.OWN);
    AuthContextHolder.set(context);

    assertThat(AuthContextHolder.current()).contains(context);
    AuthContextHolder.clear();
    assertThat(AuthContextHolder.current()).isEmpty();
  }

  @Nested
  @DisplayName("currentRowScope 메서드")
  class CurrentRowScopeTest {

    @Test
    @DisplayName("Given AuthContext 설정됨 When currentRowScope Then RowScope 반환")
    void givenContext_whenCurrentRowScope_thenReturnsRowScope() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);
      AuthContextHolder.set(context);

      assertThat(AuthContextHolder.currentRowScope()).contains(RowScope.ORG);
    }

    @Test
    @DisplayName("Given AuthContext 미설정 When currentRowScope Then empty")
    void givenNoContext_whenCurrentRowScope_thenEmpty() {
      assertThat(AuthContextHolder.currentRowScope()).isEmpty();
    }
  }

  @Nested
  @DisplayName("currentOrganizationCode 메서드")
  class CurrentOrganizationCodeTest {

    @Test
    @DisplayName("Given AuthContext 설정됨 When currentOrganizationCode Then 조직코드 반환")
    void givenContext_whenCurrentOrganizationCode_thenReturnsOrgCode() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN);
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
  @DisplayName("rowScopeSpec 메서드")
  class RowScopeSpecTest {

    private final DwOrganizationTreeService treeService = mock(DwOrganizationTreeService.class);

    @Test
    @DisplayName("Given AuthContext 미설정 When rowScopeSpec Then empty Optional")
    void givenNoContext_whenRowScopeSpec_thenEmpty() {
      var result = AuthContextHolder.rowScopeSpec("organizationCode", treeService);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given OWN scope When rowScopeSpec Then Specification 반환")
    void givenOwnScope_whenRowScopeSpec_thenReturnsSpec() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN);
      AuthContextHolder.set(context);

      var result = AuthContextHolder.rowScopeSpec("organizationCode", treeService);

      assertThat(result).isPresent();
      assertThat(result.get()).isInstanceOf(Specification.class);
    }

    @Test
    @DisplayName("Given ORG scope When rowScopeSpec Then treeService 호출하여 Specification 반환")
    void givenOrgScope_whenRowScopeSpec_thenUsesTreeService() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);
      AuthContextHolder.set(context);

      List<DwOrganizationNode> nodes = List.of(
          createNode("ORG001", null),
          createNode("ORG001_CHILD1", "ORG001"),
          createNode("ORG001_CHILD2", "ORG001")
      );
      OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.fromNodes(nodes);
      when(treeService.snapshot()).thenReturn(snapshot);

      var result = AuthContextHolder.rowScopeSpec("organizationCode", treeService);

      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Given ALL scope When rowScopeSpec Then Specification 반환")
    void givenAllScope_whenRowScopeSpec_thenReturnsSpec() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
      AuthContextHolder.set(context);

      var result = AuthContextHolder.rowScopeSpec("organizationCode", treeService);

      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Given CUSTOM scope with customSpec When rowScopeSpec Then customSpec 사용")
    void givenCustomScope_whenRowScopeSpecWithCustom_thenUsesCustomSpec() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.CUSTOM);
      AuthContextHolder.set(context);

      Specification<Object> customSpec = (root, query, cb) -> cb.isTrue(cb.literal(true));
      var result = AuthContextHolder.rowScopeSpec("organizationCode", treeService, customSpec);

      assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Given ORG scope with null treeService When rowScopeSpec Then IllegalArgumentException")
    void givenOrgScope_whenNullTreeService_thenThrowsException() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);
      AuthContextHolder.set(context);

      // ORG scope requires organizationHierarchy - null treeService results in null hierarchy
      // which triggers assertion error from RowScopeSpecifications
      org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
          AuthContextHolder.rowScopeSpec("organizationCode", null)
      );
    }

    @Test
    @DisplayName("Given overloaded rowScopeSpec without customSpec When called Then delegates correctly")
    void givenOverloadedMethod_whenCalled_thenDelegates() {
      AuthContext context = AuthContext.of(
          "user", "ORG001", "GROUP", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN);
      AuthContextHolder.set(context);

      // Test the overloaded method (without customSpecification)
      var result = AuthContextHolder.rowScopeSpec("organizationCode", treeService);
      assertThat(result).isPresent();
    }

    private DwOrganizationNode createNode(String code, String parentCode) {
      return new DwOrganizationNode(
          UUID.randomUUID(),
          code,
          1,
          code + " Name",
          parentCode,
          "ACTIVE",
          LocalDate.now(),
          null,
          UUID.randomUUID(),
          OffsetDateTime.now()
      );
    }
  }
}
