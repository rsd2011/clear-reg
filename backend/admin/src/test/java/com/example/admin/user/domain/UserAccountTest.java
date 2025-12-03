package com.example.admin.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserAccount 엔티티 테스트")
class UserAccountTest {

  @Nested
  @DisplayName("Builder 테스트")
  class BuilderTests {

    @Test
    @DisplayName("Given 모든 필드 When build Then 모든 값 설정됨")
    void givenAllFields_whenBuild_thenAllValuesSet() {
      // Given & When
      UserAccount account =
          UserAccount.builder()
              .username("testuser")
              .password("encoded-password")
              .email("test@example.com")
              .roles(Set.of("ROLE_USER", "ROLE_ADMIN"))
              .organizationCode("ORG001")
              .permissionGroupCode("PERM_GROUP")
              .employeeId("EMP001")
              .build();

      // Then
      assertThat(account.getUsername()).isEqualTo("testuser");
      assertThat(account.getPassword()).isEqualTo("encoded-password");
      assertThat(account.getEmail()).isEqualTo("test@example.com");
      assertThat(account.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
      assertThat(account.getOrganizationCode()).isEqualTo("ORG001");
      assertThat(account.getPermissionGroupCode()).isEqualTo("PERM_GROUP");
      assertThat(account.getEmployeeId()).isEqualTo("EMP001");
      assertThat(account.isActive()).isTrue();
    }

    @Test
    @DisplayName("Given null organizationCode When build Then 기본값 ROOT 설정됨")
    void givenNullOrgCode_whenBuild_thenDefaultRoot() {
      // Given & When
      UserAccount account =
          UserAccount.builder()
              .username("testuser")
              .password("password")
              .organizationCode(null)
              .build();

      // Then
      assertThat(account.getOrganizationCode()).isEqualTo("ROOT");
    }

    @Test
    @DisplayName("Given null permissionGroupCode When build Then 기본값 DEFAULT 설정됨")
    void givenNullPermGroupCode_whenBuild_thenDefaultPermGroup() {
      // Given & When
      UserAccount account =
          UserAccount.builder()
              .username("testuser")
              .password("password")
              .permissionGroupCode(null)
              .build();

      // Then
      assertThat(account.getPermissionGroupCode()).isEqualTo("DEFAULT");
    }

    @Test
    @DisplayName("Given null roles When build Then 빈 Set으로 초기화")
    void givenNullRoles_whenBuild_thenEmptySet() {
      // Given & When
      UserAccount account =
          UserAccount.builder().username("testuser").password("password").roles(null).build();

      // Then
      assertThat(account.getRoles()).isEmpty();
    }
  }

  @Nested
  @DisplayName("SSO ID 연결 테스트")
  class LinkSsoIdTests {

    @Test
    @DisplayName("Given SSO ID 없음 When linkSsoId Then 연결 성공")
    void givenNoSsoId_whenLink_thenSuccess() {
      // Given
      UserAccount account = createBasicAccount();

      // When
      account.linkSsoId("sso-id-123");

      // Then
      assertThat(account.getSsoId()).isEqualTo("sso-id-123");
    }

    @Test
    @DisplayName("Given 같은 SSO ID 연결 시도 When linkSsoId Then 성공 (idempotent)")
    void givenSameSsoId_whenLink_thenSuccess() {
      // Given
      UserAccount account = createBasicAccount();
      account.linkSsoId("sso-id-123");

      // When
      account.linkSsoId("sso-id-123");

      // Then
      assertThat(account.getSsoId()).isEqualTo("sso-id-123");
    }

    @Test
    @DisplayName("Given 다른 SSO ID 연결 시도 When linkSsoId Then IllegalStateException 발생")
    void givenDifferentSsoId_whenLink_thenThrowException() {
      // Given
      UserAccount account = createBasicAccount();
      account.linkSsoId("sso-id-123");

      // When & Then
      assertThatThrownBy(() -> account.linkSsoId("different-sso-id"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("SSO ID already linked");
    }
  }

  @Nested
  @DisplayName("AD 도메인 할당 테스트")
  class AssignAdDomainTests {

    @Test
    @DisplayName("Given AD 도메인 없음 When assignActiveDirectoryDomain Then 할당 성공")
    void givenNoAdDomain_whenAssign_thenSuccess() {
      // Given
      UserAccount account = createBasicAccount();

      // When
      account.assignActiveDirectoryDomain("CORP.LOCAL");

      // Then
      assertThat(account.getActiveDirectoryDomain()).isEqualTo("CORP.LOCAL");
    }

    @Test
    @DisplayName("Given 기존 AD 도메인 When assignActiveDirectoryDomain Then 덮어쓰기")
    void givenExistingAdDomain_whenAssign_thenOverwrite() {
      // Given
      UserAccount account = createBasicAccount();
      account.assignActiveDirectoryDomain("OLD.DOMAIN");

      // When
      account.assignActiveDirectoryDomain("NEW.DOMAIN");

      // Then
      assertThat(account.getActiveDirectoryDomain()).isEqualTo("NEW.DOMAIN");
    }
  }

  @Nested
  @DisplayName("비밀번호 업데이트 테스트")
  class UpdatePasswordTests {

    @Test
    @DisplayName("Given 새 비밀번호 When updatePassword Then 비밀번호 및 변경 시간 갱신")
    void givenNewPassword_whenUpdate_thenPasswordAndTimeUpdated() {
      // Given
      UserAccount account = createBasicAccount();
      Instant before = Instant.now().minusSeconds(1);

      // When
      account.updatePassword("new-encoded-password");

      // Then
      assertThat(account.getPassword()).isEqualTo("new-encoded-password");
      assertThat(account.getPasswordChangedAt()).isAfter(before);
    }
  }

  @Nested
  @DisplayName("계정 잠금 테스트")
  class LockTests {

    @Test
    @DisplayName("Given 잠금 시간 미래 When isLocked Then true")
    void givenFutureLockTime_whenIsLocked_thenTrue() {
      // Given
      UserAccount account = createBasicAccount();
      account.lockUntil(Instant.now().plusSeconds(3600));

      // When & Then
      assertThat(account.isLocked()).isTrue();
    }

    @Test
    @DisplayName("Given 잠금 시간 과거 When isLocked Then false")
    void givenPastLockTime_whenIsLocked_thenFalse() {
      // Given
      UserAccount account = createBasicAccount();
      account.lockUntil(Instant.now().minusSeconds(3600));

      // When & Then
      assertThat(account.isLocked()).isFalse();
    }

    @Test
    @DisplayName("Given 잠금 시간 null When isLocked Then false")
    void givenNullLockTime_whenIsLocked_thenFalse() {
      // Given
      UserAccount account = createBasicAccount();

      // When & Then
      assertThat(account.isLocked()).isFalse();
    }

    @Test
    @DisplayName("Given 잠긴 계정 When unlock Then 잠금 해제 및 실패 횟수 초기화")
    void givenLockedAccount_whenUnlock_thenUnlockedAndResetAttempts() {
      // Given
      UserAccount account = createBasicAccount();
      account.lockUntil(Instant.now().plusSeconds(3600));
      account.incrementFailedAttempt();
      account.incrementFailedAttempt();

      // When
      account.unlock();

      // Then
      assertThat(account.getLockedUntil()).isNull();
      assertThat(account.getFailedLoginAttempts()).isZero();
    }
  }

  @Nested
  @DisplayName("로그인 실패 횟수 테스트")
  class FailedAttemptsTests {

    @Test
    @DisplayName("Given 계정 When incrementFailedAttempt Then 횟수 증가")
    void givenAccount_whenIncrement_thenIncreased() {
      // Given
      UserAccount account = createBasicAccount();
      assertThat(account.getFailedLoginAttempts()).isZero();

      // When
      account.incrementFailedAttempt();
      account.incrementFailedAttempt();

      // Then
      assertThat(account.getFailedLoginAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given 실패 횟수 있음 When resetFailedAttempts Then 0으로 초기화")
    void givenFailedAttempts_whenReset_thenZero() {
      // Given
      UserAccount account = createBasicAccount();
      account.incrementFailedAttempt();
      account.incrementFailedAttempt();

      // When
      account.resetFailedAttempts();

      // Then
      assertThat(account.getFailedLoginAttempts()).isZero();
    }
  }

  @Nested
  @DisplayName("계정 활성화/비활성화 테스트")
  class ActivateDeactivateTests {

    @Test
    @DisplayName("Given 활성 계정 When deactivate Then 비활성화")
    void givenActiveAccount_whenDeactivate_thenInactive() {
      // Given
      UserAccount account = createBasicAccount();
      assertThat(account.isActive()).isTrue();

      // When
      account.deactivate();

      // Then
      assertThat(account.isActive()).isFalse();
    }

    @Test
    @DisplayName("Given 비활성 계정 When activate Then 활성화 및 잠금 해제")
    void givenInactiveAccount_whenActivate_thenActiveAndUnlocked() {
      // Given
      UserAccount account = createBasicAccount();
      account.deactivate();
      account.lockUntil(Instant.now().plusSeconds(3600));
      account.incrementFailedAttempt();

      // When
      account.activate();

      // Then
      assertThat(account.isActive()).isTrue();
      assertThat(account.getLockedUntil()).isNull();
      assertThat(account.getFailedLoginAttempts()).isZero();
    }
  }

  @Nested
  @DisplayName("업데이트 메서드 테스트")
  class UpdateMethodsTests {

    @Test
    @DisplayName("Given 이메일 When updateEmail Then 이메일 변경됨")
    void givenEmail_whenUpdate_thenChanged() {
      // Given
      UserAccount account = createBasicAccount();

      // When
      account.updateEmail("new@example.com");

      // Then
      assertThat(account.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Given 조직 코드 When updateOrganizationCode Then 변경됨")
    void givenOrgCode_whenUpdate_thenChanged() {
      // Given
      UserAccount account = createBasicAccount();

      // When
      account.updateOrganizationCode("NEW_ORG");

      // Then
      assertThat(account.getOrganizationCode()).isEqualTo("NEW_ORG");
    }

    @Test
    @DisplayName("Given 권한 그룹 코드 When updatePermissionGroupCode Then 변경됨")
    void givenPermGroupCode_whenUpdate_thenChanged() {
      // Given
      UserAccount account = createBasicAccount();

      // When
      account.updatePermissionGroupCode("NEW_PERM_GROUP");

      // Then
      assertThat(account.getPermissionGroupCode()).isEqualTo("NEW_PERM_GROUP");
    }

    @Test
    @DisplayName("Given 사번 When updateEmployeeId Then 변경됨")
    void givenEmployeeId_whenUpdate_thenChanged() {
      // Given
      UserAccount account = createBasicAccount();

      // When
      account.updateEmployeeId("EMP002");

      // Then
      assertThat(account.getEmployeeId()).isEqualTo("EMP002");
    }

    @Test
    @DisplayName("Given 로그인 시간 When updateLastLoginAt Then 변경됨")
    void givenLastLoginAt_whenUpdate_thenChanged() {
      // Given
      UserAccount account = createBasicAccount();
      Instant loginTime = Instant.now();

      // When
      account.updateLastLoginAt(loginTime);

      // Then
      assertThat(account.getLastLoginAt()).isEqualTo(loginTime);
    }
  }

  private UserAccount createBasicAccount() {
    return UserAccount.builder()
        .username("testuser")
        .password("encoded-password")
        .organizationCode("ORG001")
        .build();
  }
}
