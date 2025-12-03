package com.example.server.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.service.PermissionEvaluator;
import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.service.UserAccountService;
import com.example.auth.LoginType;
import com.example.auth.security.PolicyToggleProvider;
import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;
import com.example.server.user.dto.PasswordChangeRequest;
import com.example.server.user.dto.UserCreateRequest;
import com.example.server.user.dto.UserUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * UserController 테스트.
 */
@WebMvcTest(
    controllers = {UserController.class, com.example.server.web.GlobalExceptionHandler.class},
    excludeFilters = @Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
            RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 테스트")
class UserControllerTest {

  private static final String BASE_URL = "/api/v1/users";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private UserAccountService userAccountService;

  @MockBean
  private PermissionEvaluator permissionEvaluator;

  @MockBean
  private PolicyToggleProvider policyToggleProvider;

  @MockBean
  private RowAccessPolicyProvider rowAccessPolicyProvider;

  @MockBean
  private DwOrganizationQueryService organizationQueryService;

  private UUID testUserId;
  private UserAccount testUserAccount;

  @BeforeEach
  void setUp() {
    AuthContextHolder.set(AuthContext.of("admin", "ORG-001", "ADMIN_GROUP",
        FeatureCode.USER, ActionCode.READ, List.of()));

    testUserId = UUID.randomUUID();
    testUserAccount = createTestUserAccount(testUserId, "testuser", "ORG-001");

    // 기본 권한 설정 - 모든 조직 접근 가능
    given(rowAccessPolicyProvider.evaluate(any()))
        .willReturn(Optional.of(RowAccessMatch.builder()
            .rowScope(RowScope.ALL)
            .build()));
    given(organizationQueryService.getOrganizations(any(), eq(RowScope.ALL), any()))
        .willReturn(new PageImpl<>(List.of()));
  }

  @AfterEach
  void tearDown() {
    AuthContextHolder.clear();
  }

  @Nested
  @DisplayName("사용자 목록 조회")
  class ListUsersTest {

    @Test
    @DisplayName("Given 검색 조건 없이 요청 When GET /api/v1/users Then 사용자 목록을 반환한다")
    void givenNoFilter_whenListUsers_thenReturnsPagedUsers() throws Exception {
      given(userAccountService.findAll(any(), any(Pageable.class)))
          .willReturn(new PageImpl<>(List.of(testUserAccount)));

      mockMvc.perform(get(BASE_URL))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content[0].username").value("testuser"));
    }

    @Test
    @DisplayName("Given 검색 조건 When GET /api/v1/users Then 필터링된 목록을 반환한다")
    void givenSearchCriteria_whenListUsers_thenReturnsFilteredUsers() throws Exception {
      given(userAccountService.findAll(any(), any(Pageable.class)))
          .willReturn(new PageImpl<>(List.of(testUserAccount)));

      mockMvc.perform(get(BASE_URL)
              .param("username", "test")
              .param("organizationCode", "ORG-001")
              .param("active", "true"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray());
    }
  }

  @Nested
  @DisplayName("사용자 상세 조회")
  class GetUserTest {

    @Test
    @DisplayName("Given 사용자 ID When GET /api/v1/users/{id} Then 사용자 상세를 반환한다")
    void givenUserId_whenGetUser_thenReturnsUserDetail() throws Exception {
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(testUserAccount);

      mockMvc.perform(get(BASE_URL + "/" + testUserId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("testuser"))
          .andExpect(jsonPath("$.email").value("test@example.com"));
    }
  }

  @Nested
  @DisplayName("사용자 생성")
  class CreateUserTest {

    @Test
    @DisplayName("Given 유효한 생성 요청 When POST /api/v1/users Then 생성된 사용자를 반환한다")
    void givenValidRequest_whenCreateUser_thenReturnsCreatedUser() throws Exception {
      UserCreateRequest request = new UserCreateRequest(
          "newuser", "new@example.com", "ORG-001", "USER_GROUP",
          "EMP-001", Set.of(), "password123!");

      given(userAccountService.existsByUsername("newuser")).willReturn(false);
      given(userAccountService.encodePassword(any())).willReturn("encoded_password");
      given(userAccountService.save(any())).willReturn(
          createTestUserAccount(UUID.randomUUID(), "newuser", "ORG-001"));
      given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.PASSWORD));

      mockMvc.perform(post(BASE_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.username").value("newuser"));

      verify(userAccountService).save(any());
    }

    @Test
    @DisplayName("Given 중복된 사용자명 When POST /api/v1/users Then 400 에러를 반환한다")
    void givenDuplicateUsername_whenCreateUser_thenReturnsBadRequest() throws Exception {
      UserCreateRequest request = new UserCreateRequest(
          "existinguser", "new@example.com", "ORG-001", "USER_GROUP",
          "EMP-001", Set.of(), "password123!");

      given(userAccountService.existsByUsername("existinguser")).willReturn(true);

      mockMvc.perform(post(BASE_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());

      verify(userAccountService, never()).save(any());
    }
  }

  @Nested
  @DisplayName("사용자 수정")
  class UpdateUserTest {

    @Test
    @DisplayName("Given 수정 요청 When PUT /api/v1/users/{id} Then 수정된 사용자를 반환한다")
    void givenUpdateRequest_whenUpdateUser_thenReturnsUpdatedUser() throws Exception {
      UserUpdateRequest request = new UserUpdateRequest(
          "updated@example.com", "ORG-002", "NEW_GROUP", "EMP-002", Set.of());

      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(testUserAccount);
      given(userAccountService.save(any())).willReturn(testUserAccount);

      mockMvc.perform(put(BASE_URL + "/" + testUserId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());

      verify(userAccountService).save(any());
    }
  }

  @Nested
  @DisplayName("사용자 삭제")
  class DeleteUserTest {

    @Test
    @DisplayName("Given 사용자 ID When DELETE /api/v1/users/{id} Then 204 NoContent를 반환한다")
    void givenUserId_whenDeleteUser_thenReturnsNoContent() throws Exception {
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(testUserAccount);

      mockMvc.perform(delete(BASE_URL + "/" + testUserId))
          .andExpect(status().isNoContent());

      verify(userAccountService).softDelete(testUserId);
    }

    @Test
    @DisplayName("Given 본인 계정 삭제 시도 When DELETE /api/v1/users/{id} Then 400 에러를 반환한다")
    void givenSelfDeletion_whenDeleteUser_thenReturnsBadRequest() throws Exception {
      // admin 사용자가 자기 자신을 삭제하려고 시도
      UserAccount adminAccount = createTestUserAccount(testUserId, "admin", "ORG-001");
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(adminAccount);

      mockMvc.perform(delete(BASE_URL + "/" + testUserId))
          .andExpect(status().isBadRequest());

      verify(userAccountService, never()).softDelete(any());
    }
  }

  @Nested
  @DisplayName("사용자 활성화/비활성화")
  class ActivationTest {

    @Test
    @DisplayName("Given 비활성화된 사용자 When POST /api/v1/users/{id}/activate Then 활성화된다")
    void givenInactiveUser_whenActivate_thenUserIsActivated() throws Exception {
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(testUserAccount);

      mockMvc.perform(post(BASE_URL + "/" + testUserId + "/activate"))
          .andExpect(status().isOk());

      verify(userAccountService).activate("testuser");
    }

    @Test
    @DisplayName("Given 활성화된 사용자 When POST /api/v1/users/{id}/deactivate Then 비활성화된다")
    void givenActiveUser_whenDeactivate_thenUserIsDeactivated() throws Exception {
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(testUserAccount);

      mockMvc.perform(post(BASE_URL + "/" + testUserId + "/deactivate"))
          .andExpect(status().isOk());

      verify(userAccountService).deactivate("testuser");
    }
  }

  @Nested
  @DisplayName("비밀번호 초기화")
  class ResetPasswordTest {

    @BeforeEach
    void setUp() {
      AuthContextHolder.set(AuthContext.of("admin", "ORG-001", "ADMIN_GROUP",
          FeatureCode.USER, ActionCode.ADMIN, List.of()));
    }

    @Test
    @DisplayName("Given 일반로그인 활성화 When POST /api/v1/users/{id}/reset-password Then 임시 비밀번호 반환")
    void givenPasswordLoginEnabled_whenResetPassword_thenReturnsTempPassword() throws Exception {
      given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.PASSWORD));
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(testUserAccount);
      given(userAccountService.encodePassword(any())).willReturn("encoded_temp_password");

      mockMvc.perform(post(BASE_URL + "/" + testUserId + "/reset-password"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());

      verify(userAccountService).resetPassword(eq("testuser"), any());
    }

    @Test
    @DisplayName("Given 일반로그인 비활성화 When POST /api/v1/users/{id}/reset-password Then 500 에러 반환")
    void givenPasswordLoginDisabled_whenResetPassword_thenReturnsError() throws Exception {
      given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.SSO));

      mockMvc.perform(post(BASE_URL + "/" + testUserId + "/reset-password"))
          .andExpect(status().isInternalServerError());

      verify(userAccountService, never()).resetPassword(any(), any());
    }
  }

  @Nested
  @DisplayName("계정 잠금 해제")
  class UnlockUserTest {

    @BeforeEach
    void setUp() {
      AuthContextHolder.set(AuthContext.of("admin", "ORG-001", "ADMIN_GROUP",
          FeatureCode.USER, ActionCode.ADMIN, List.of()));
    }

    @Test
    @DisplayName("Given 잠긴 사용자 When POST /api/v1/users/{id}/unlock Then 잠금이 해제된다")
    void givenLockedUser_whenUnlock_thenUserIsUnlocked() throws Exception {
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(testUserAccount);

      mockMvc.perform(post(BASE_URL + "/" + testUserId + "/unlock"))
          .andExpect(status().isOk());

      verify(userAccountService).unlock("testuser");
    }
  }

  @Nested
  @DisplayName("내 정보 API")
  class MyProfileTest {

    @Test
    @DisplayName("Given 로그인 상태 When GET /api/v1/users/me Then 내 정보를 반환한다")
    void givenAuthenticated_whenGetMyProfile_thenReturnsMyProfile() throws Exception {
      UserAccount adminAccount = createTestUserAccount(UUID.randomUUID(), "admin", "ORG-001");
      given(userAccountService.getByUsernameOrThrow("admin")).willReturn(adminAccount);

      mockMvc.perform(get(BASE_URL + "/me"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("Given 일반로그인 활성화 When PUT /api/v1/users/me/password Then 비밀번호가 변경된다")
    void givenPasswordLoginEnabled_whenChangePassword_thenPasswordIsChanged() throws Exception {
      PasswordChangeRequest request = new PasswordChangeRequest("currentPass123!", "newPass456!");

      given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.PASSWORD));
      UserAccount adminAccount = createTestUserAccount(UUID.randomUUID(), "admin", "ORG-001");
      given(userAccountService.getByUsernameOrThrow("admin")).willReturn(adminAccount);
      given(userAccountService.passwordMatches(any(), eq("currentPass123!"))).willReturn(true);
      given(userAccountService.encodePassword("newPass456!")).willReturn("encoded_new_password");

      mockMvc.perform(put(BASE_URL + "/me/password")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());

      verify(userAccountService).updatePassword(eq("admin"), any());
    }

    @Test
    @DisplayName("Given 현재 비밀번호 불일치 When PUT /api/v1/users/me/password Then 400 에러 반환")
    void givenWrongCurrentPassword_whenChangePassword_thenReturnsBadRequest() throws Exception {
      PasswordChangeRequest request = new PasswordChangeRequest("wrongPass", "newPass456!");

      given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.PASSWORD));
      UserAccount adminAccount = createTestUserAccount(UUID.randomUUID(), "admin", "ORG-001");
      given(userAccountService.getByUsernameOrThrow("admin")).willReturn(adminAccount);
      given(userAccountService.passwordMatches(any(), eq("wrongPass"))).willReturn(false);

      mockMvc.perform(put(BASE_URL + "/me/password")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());

      verify(userAccountService, never()).updatePassword(any(), any());
    }

    @Test
    @DisplayName("Given 일반로그인 비활성화 When PUT /api/v1/users/me/password Then 500 에러 반환")
    void givenPasswordLoginDisabled_whenChangePassword_thenReturnsError() throws Exception {
      PasswordChangeRequest request = new PasswordChangeRequest("currentPass", "newPass456!");

      given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.SSO));

      mockMvc.perform(put(BASE_URL + "/me/password")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isInternalServerError());

      verify(userAccountService, never()).updatePassword(any(), any());
    }
  }

  @Nested
  @DisplayName("RowScope 접근 제한")
  class RowScopeAccessTest {

    @Test
    @DisplayName("Given ORG scope When 다른 조직 사용자 조회 Then 403 에러 반환")
    void givenOrgScope_whenAccessOtherOrgUser_thenReturnsForbidden() throws Exception {
      // ORG scope 설정
      given(rowAccessPolicyProvider.evaluate(any()))
          .willReturn(Optional.of(RowAccessMatch.builder()
              .rowScope(RowScope.ORG)
              .build()));

      // 조회자 조직: ORG-001, 대상 사용자 조직: ORG-002
      DwOrganizationNode org1 = createOrganizationNode("ORG-001", "조직1");
      given(organizationQueryService.getOrganizations(any(), eq(RowScope.ORG), eq("ORG-001")))
          .willReturn(new PageImpl<>(List.of(org1)));

      UserAccount otherOrgUser = createTestUserAccount(testUserId, "otheruser", "ORG-002");
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(otherOrgUser);

      mockMvc.perform(get(BASE_URL + "/" + testUserId))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Given ALL scope When 다른 조직 사용자 조회 Then 성공적으로 반환")
    void givenAllScope_whenAccessOtherOrgUser_thenReturnsUser() throws Exception {
      given(rowAccessPolicyProvider.evaluate(any()))
          .willReturn(Optional.of(RowAccessMatch.builder()
              .rowScope(RowScope.ALL)
              .build()));

      UserAccount otherOrgUser = createTestUserAccount(testUserId, "otheruser", "ORG-002");
      given(userAccountService.getByIdOrThrow(testUserId)).willReturn(otherOrgUser);

      mockMvc.perform(get(BASE_URL + "/" + testUserId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username").value("otheruser"));
    }
  }

  private UserAccount createTestUserAccount(UUID id, String username, String organizationCode) {
    UserAccount account = UserAccount.builder()
        .username(username)
        .password("encoded_password")
        .email("test@example.com")
        .organizationCode(organizationCode)
        .permissionGroupCode("DEFAULT")
        .employeeId("EMP-001")
        .roles(new HashSet<>())
        .build();

    // ID는 부모 클래스 PrimaryKeyEntity에 있으므로 상위 클래스에서 찾아야 함
    try {
      var idField = account.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(account, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    account.updateLastLoginAt(Instant.now());
    return account;
  }

  private DwOrganizationNode createOrganizationNode(String code, String name) {
    return new DwOrganizationNode(
        UUID.randomUUID(),
        code,
        1,
        name,
        null,
        "ACTIVE",
        LocalDate.now(),
        null,
        UUID.randomUUID(),
        OffsetDateTime.now()
    );
  }
}
