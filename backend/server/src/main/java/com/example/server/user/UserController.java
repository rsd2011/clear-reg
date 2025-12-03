package com.example.server.user;

import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.service.UserAccountService;
import com.example.auth.LoginType;
import com.example.auth.security.PolicyToggleProvider;
import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.policy.RowAccessQuery;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;
import com.example.server.user.dto.MyProfileResponse;
import com.example.server.user.dto.PasswordChangeRequest;
import com.example.server.user.dto.UserCreateRequest;
import com.example.server.user.dto.UserDetailResponse;
import com.example.server.user.dto.UserListResponse;
import com.example.server.user.dto.UserSearchCriteria;
import com.example.server.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 관리 API 컨트롤러.
 *
 * <p>사용자 CRUD 및 계정 관리 기능을 제공합니다.
 * RowScope가 적용되어 조직 범위에 따라 조회 가능한 사용자가 제한됩니다.
 */
@RestController
@Validated
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "사용자 관리 API")
public class UserController {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String TEMP_PASSWORD_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
  private static final int TEMP_PASSWORD_LENGTH = 12;
  private static final RowScope DEFAULT_ROW_SCOPE = RowScope.ORG;

  private final UserAccountService userAccountService;
  private final PolicyToggleProvider policyToggleProvider;
  private final RowAccessPolicyProvider rowAccessPolicyProvider;
  private final DwOrganizationQueryService organizationQueryService;

  public UserController(
      UserAccountService userAccountService,
      PolicyToggleProvider policyToggleProvider,
      RowAccessPolicyProvider rowAccessPolicyProvider,
      DwOrganizationQueryService organizationQueryService) {
    this.userAccountService = userAccountService;
    this.policyToggleProvider = policyToggleProvider;
    this.rowAccessPolicyProvider = rowAccessPolicyProvider;
    this.organizationQueryService = organizationQueryService;
  }

  /**
   * 사용자 목록을 조회합니다.
   * RowScope에 따라 조회 가능한 사용자가 제한됩니다.
   */
  @GetMapping
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.READ)
  @Operation(summary = "사용자 목록 조회", description = "RowScope에 따라 조회 가능한 사용자 목록을 반환합니다")
  public Page<UserListResponse> listUsers(
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String organizationCode,
      @RequestParam(required = false) String permissionGroupCode,
      @RequestParam(required = false) Boolean active,
      Pageable pageable) {

    AuthContext context = currentContext();
    RowScope rowScope = effectiveRowScope(resolveRowScope(context));
    Collection<String> scopedOrganizations = resolveOrganizations(rowScope, context.organizationCode());

    UserSearchCriteria criteria = new UserSearchCriteria(
        username, email, organizationCode, permissionGroupCode, active);

    Specification<UserAccount> spec = UserAccountSpecifications.withCriteriaAndRowScope(
        criteria,
        rowScope,
        context.organizationCode(),
        scopedOrganizations
    );

    return userAccountService.findAll(spec, pageable)
        .map(UserListResponse::from);
  }

  /**
   * 사용자 상세 정보를 조회합니다.
   */
  @GetMapping("/{id}")
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.READ)
  @Operation(summary = "사용자 상세 조회", description = "사용자 상세 정보를 반환합니다")
  public UserDetailResponse getUser(@PathVariable UUID id) {
    AuthContext context = currentContext();
    UserAccount account = userAccountService.getByIdOrThrow(id);
    validateRowScopeAccess(context, account);
    return UserDetailResponse.from(account);
  }

  /**
   * 새 사용자를 생성합니다.
   *
   * <p>비밀번호 설정은 일반로그인 정책이 활성화된 경우에만 가능합니다.
   * 일반로그인 정책이 비활성화된 경우 비밀번호 필드는 무시됩니다.
   */
  @PostMapping
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.CREATE)
  @Operation(summary = "사용자 생성",
      description = "새 사용자를 생성합니다. 비밀번호 설정은 일반로그인 정책 활성화 시에만 가능합니다.")
  public ResponseEntity<UserDetailResponse> createUser(
      @Valid @RequestBody UserCreateRequest request) {

    if (userAccountService.existsByUsername(request.username())) {
      throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + request.username());
    }

    String encodedPassword;
    if (isPasswordLoginEnabled() && request.password() != null) {
      encodedPassword = userAccountService.encodePassword(request.password());
    } else {
      // 비밀번호 로그인이 비활성화된 경우 랜덤 비밀번호 설정
      encodedPassword = userAccountService.encodePassword(generateTempPassword());
    }

    UserAccount account = UserAccount.builder()
        .username(request.username())
        .password(encodedPassword)
        .email(request.email())
        .organizationCode(request.organizationCode())
        .permissionGroupCode(request.permissionGroupCode())
        .employeeId(request.employeeId())
        .roles(request.roles() != null ? request.roles() : new HashSet<>())
        .build();

    UserAccount saved = userAccountService.save(account);
    return ResponseEntity.status(HttpStatus.CREATED).body(UserDetailResponse.from(saved));
  }

  /**
   * 사용자 정보를 수정합니다.
   */
  @PutMapping("/{id}")
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.UPDATE)
  @Operation(summary = "사용자 수정", description = "사용자 정보를 수정합니다")
  public UserDetailResponse updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody UserUpdateRequest request) {

    AuthContext context = currentContext();
    UserAccount account = userAccountService.getByIdOrThrow(id);
    validateRowScopeAccess(context, account);

    if (request.email() != null) {
      account.updateEmail(request.email());
    }
    if (request.organizationCode() != null) {
      account.updateOrganizationCode(request.organizationCode());
    }
    if (request.permissionGroupCode() != null) {
      account.updatePermissionGroupCode(request.permissionGroupCode());
    }
    if (request.employeeId() != null) {
      account.updateEmployeeId(request.employeeId());
    }

    UserAccount saved = userAccountService.save(account);
    return UserDetailResponse.from(saved);
  }

  /**
   * 사용자를 삭제(비활성화)합니다. Soft delete로 처리됩니다.
   */
  @DeleteMapping("/{id}")
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.DELETE)
  @Operation(summary = "사용자 삭제", description = "사용자를 비활성화합니다 (Soft Delete)")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
    AuthContext context = currentContext();
    UserAccount account = userAccountService.getByIdOrThrow(id);
    validateRowScopeAccess(context, account);
    validateNotSelf(context, account);

    userAccountService.softDelete(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * 사용자를 활성화합니다.
   */
  @PostMapping("/{id}/activate")
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.UPDATE)
  @Operation(summary = "사용자 활성화", description = "비활성화된 사용자를 활성화합니다")
  public UserDetailResponse activateUser(@PathVariable UUID id) {
    AuthContext context = currentContext();
    UserAccount account = userAccountService.getByIdOrThrow(id);
    validateRowScopeAccess(context, account);

    userAccountService.activate(account.getUsername());
    return UserDetailResponse.from(userAccountService.getByIdOrThrow(id));
  }

  /**
   * 사용자를 비활성화합니다.
   */
  @PostMapping("/{id}/deactivate")
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.UPDATE)
  @Operation(summary = "사용자 비활성화", description = "사용자를 비활성화합니다")
  public UserDetailResponse deactivateUser(@PathVariable UUID id) {
    AuthContext context = currentContext();
    UserAccount account = userAccountService.getByIdOrThrow(id);
    validateRowScopeAccess(context, account);
    validateNotSelf(context, account);

    userAccountService.deactivate(account.getUsername());
    return UserDetailResponse.from(userAccountService.getByIdOrThrow(id));
  }

  /**
   * 사용자 비밀번호를 초기화합니다.
   *
   * <p>일반로그인 정책이 활성화된 경우에만 사용 가능합니다.
   * 임시 비밀번호가 생성되어 반환됩니다.
   */
  @PostMapping("/{id}/reset-password")
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.ADMIN)
  @Operation(summary = "비밀번호 초기화",
      description = "사용자 비밀번호를 초기화합니다. 일반로그인 정책이 활성화된 경우에만 사용 가능합니다.")
  public ResponseEntity<PasswordResetResponse> resetPassword(@PathVariable UUID id) {
    validatePasswordLoginEnabled();

    AuthContext context = currentContext();
    UserAccount account = userAccountService.getByIdOrThrow(id);
    validateRowScopeAccess(context, account);

    String tempPassword = generateTempPassword();
    String encodedPassword = userAccountService.encodePassword(tempPassword);
    userAccountService.resetPassword(account.getUsername(), encodedPassword);

    return ResponseEntity.ok(new PasswordResetResponse(tempPassword));
  }

  /**
   * 사용자 계정 잠금을 해제합니다.
   */
  @PostMapping("/{id}/unlock")
  @RequirePermission(feature = FeatureCode.USER, action = ActionCode.ADMIN)
  @Operation(summary = "계정 잠금 해제", description = "잠긴 사용자 계정을 해제합니다")
  public UserDetailResponse unlockUser(@PathVariable UUID id) {
    AuthContext context = currentContext();
    UserAccount account = userAccountService.getByIdOrThrow(id);
    validateRowScopeAccess(context, account);

    userAccountService.unlock(account.getUsername());
    return UserDetailResponse.from(userAccountService.getByIdOrThrow(id));
  }

  // ==================== 내 정보 API ====================

  /**
   * 내 정보를 조회합니다.
   */
  @GetMapping("/me")
  @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 반환합니다")
  public MyProfileResponse getMyProfile() {
    String username = currentUsername();
    UserAccount account = userAccountService.getByUsernameOrThrow(username);
    return MyProfileResponse.from(account);
  }

  /**
   * 내 비밀번호를 변경합니다.
   *
   * <p>일반로그인 정책이 활성화된 경우에만 사용 가능합니다.
   */
  @PutMapping("/me/password")
  @Operation(summary = "내 비밀번호 변경",
      description = "로그인한 사용자의 비밀번호를 변경합니다. 일반로그인 정책이 활성화된 경우에만 사용 가능합니다.")
  public ResponseEntity<Void> changeMyPassword(
      @Valid @RequestBody PasswordChangeRequest request) {
    validatePasswordLoginEnabled();

    String username = currentUsername();
    UserAccount account = userAccountService.getByUsernameOrThrow(username);

    if (!userAccountService.passwordMatches(account, request.currentPassword())) {
      throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
    }

    String encodedPassword = userAccountService.encodePassword(request.newPassword());
    userAccountService.updatePassword(username, encodedPassword);

    return ResponseEntity.ok().build();
  }

  // ==================== Helper Methods ====================

  private AuthContext currentContext() {
    return AuthContextHolder.current()
        .orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
  }

  private String currentUsername() {
    return currentContext().username();
  }

  private boolean isPasswordLoginEnabled() {
    List<LoginType> enabledTypes = policyToggleProvider.enabledLoginTypes();
    return enabledTypes.contains(LoginType.PASSWORD);
  }

  private void validatePasswordLoginEnabled() {
    if (!isPasswordLoginEnabled()) {
      throw new IllegalStateException("일반로그인 정책이 비활성화되어 있어 비밀번호 관련 기능을 사용할 수 없습니다");
    }
  }

  private RowScope resolveRowScope(AuthContext context) {
    RowAccessQuery query = new RowAccessQuery(
        context.feature() != null ? context.feature().name() : null,
        context.action() != null ? context.action().name() : null,
        context.permissionGroupCode(),
        context.orgGroupCodes(),
        null);
    return rowAccessPolicyProvider.evaluate(query)
        .map(RowAccessMatch::getRowScope)
        .orElse(DEFAULT_ROW_SCOPE);
  }

  private RowScope effectiveRowScope(RowScope requested) {
    if (requested == null) {
      return DEFAULT_ROW_SCOPE;
    }
    if (requested == RowScope.OWN) {
      return RowScope.ORG;
    }
    return requested;
  }

  private Collection<String> resolveOrganizations(RowScope rowScope, String organizationCode) {
    if (rowScope == RowScope.ALL) {
      return List.of();
    }
    return organizationQueryService.getOrganizations(Pageable.unpaged(), rowScope, organizationCode)
        .map(DwOrganizationNode::organizationCode)
        .getContent();
  }

  private void validateRowScopeAccess(AuthContext context, UserAccount account) {
    RowScope rowScope = effectiveRowScope(resolveRowScope(context));
    Collection<String> scopedOrganizations = resolveOrganizations(rowScope, context.organizationCode());

    switch (rowScope) {
      case ALL -> {
        // 모든 사용자 접근 가능
      }
      case ORG -> {
        if (!scopedOrganizations.contains(account.getOrganizationCode())) {
          throw new PermissionDeniedException("해당 사용자에 대한 접근 권한이 없습니다");
        }
      }
      case OWN -> {
        if (!context.organizationCode().equals(account.getOrganizationCode())) {
          throw new PermissionDeniedException("해당 사용자에 대한 접근 권한이 없습니다");
        }
      }
      case CUSTOM -> {
        // CUSTOM은 별도 처리 필요 - 현재는 허용
      }
    }
  }

  private void validateNotSelf(AuthContext context, UserAccount account) {
    if (context.username().equals(account.getUsername())) {
      throw new IllegalArgumentException("본인 계정은 비활성화하거나 삭제할 수 없습니다");
    }
  }

  private String generateTempPassword() {
    StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
    for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
      sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }

  /**
   * 비밀번호 초기화 응답.
   */
  public record PasswordResetResponse(String temporaryPassword) {
  }
}
