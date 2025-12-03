package com.example.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.exception.UserNotFoundException;
import com.example.admin.user.repository.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("UserAccountService 테스트")
@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  @Mock private UserAccountRepository repository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserAccountService service;

  @BeforeEach
  void setUp() {
    service = new UserAccountService(repository, passwordEncoder);
  }

  @Nested
  @DisplayName("getByUsernameOrThrow 메서드")
  class GetByUsernameOrThrowTests {

    @Test
    @DisplayName("Given 존재하는 사용자 When getByUsernameOrThrow Then 사용자 반환")
    void givenExistingUser_whenGet_thenReturnUser() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));

      // When
      UserAccount result = service.getByUsernameOrThrow("testuser");

      // Then
      assertThat(result).isEqualTo(account);
    }

    @Test
    @DisplayName("Given 존재하지 않는 사용자 When getByUsernameOrThrow Then UserNotFoundException 발생")
    void givenNonExistingUser_whenGet_thenThrowException() {
      // Given
      when(repository.findByUsername("nonexistent")).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.getByUsernameOrThrow("nonexistent"))
          .isInstanceOf(UserNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findByUsername 메서드")
  class FindByUsernameTests {

    @Test
    @DisplayName("Given 존재하는 사용자 When findByUsername Then Optional with user")
    void givenExistingUser_whenFind_thenReturnOptionalWithUser() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));

      // When
      Optional<UserAccount> result = service.findByUsername("testuser");

      // Then
      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo(account);
    }

    @Test
    @DisplayName("Given 존재하지 않는 사용자 When findByUsername Then empty Optional")
    void givenNonExistingUser_whenFind_thenReturnEmpty() {
      // Given
      when(repository.findByUsername("nonexistent")).thenReturn(Optional.empty());

      // When
      Optional<UserAccount> result = service.findByUsername("nonexistent");

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("findBySsoId 메서드")
  class FindBySsoIdTests {

    @Test
    @DisplayName("Given SSO ID 존재 When findBySsoId Then 사용자 반환")
    void givenExistingSsoId_whenFind_thenReturnUser() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.findBySsoId("sso-id-123")).thenReturn(Optional.of(account));

      // When
      Optional<UserAccount> result = service.findBySsoId("sso-id-123");

      // Then
      assertThat(result).isPresent();
    }
  }

  @Nested
  @DisplayName("findByPermissionGroupCodeIn 메서드")
  class FindByPermissionGroupCodeInTests {

    @Test
    @DisplayName("Given 권한 그룹 코드 목록 When findByPermissionGroupCodeIn Then 사용자 목록 반환")
    void givenCodes_whenFind_thenReturnUsers() {
      // Given
      List<String> codes = List.of("GROUP_A", "GROUP_B");
      UserAccount account1 = createUserAccount("user1");
      UserAccount account2 = createUserAccount("user2");
      when(repository.findByPermissionGroupCodeIn(codes)).thenReturn(List.of(account1, account2));

      // When
      List<UserAccount> result = service.findByPermissionGroupCodeIn(codes);

      // Then
      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("save 메서드")
  class SaveTests {

    @Test
    @DisplayName("Given 사용자 When save Then 저장된 사용자 반환")
    void givenUser_whenSave_thenReturnSavedUser() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.save(account)).thenReturn(account);

      // When
      UserAccount result = service.save(account);

      // Then
      assertThat(result).isEqualTo(account);
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("passwordMatches 메서드")
  class PasswordMatchesTests {

    @Test
    @DisplayName("Given 일치하는 비밀번호 When passwordMatches Then true")
    void givenMatchingPassword_whenCheck_thenTrue() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(passwordEncoder.matches("rawPassword", "encoded-password")).thenReturn(true);

      // When
      boolean result = service.passwordMatches(account, "rawPassword");

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 불일치하는 비밀번호 When passwordMatches Then false")
    void givenNonMatchingPassword_whenCheck_thenFalse() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(passwordEncoder.matches("wrongPassword", "encoded-password")).thenReturn(false);

      // When
      boolean result = service.passwordMatches(account, "wrongPassword");

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("updatePassword 메서드")
  class UpdatePasswordTests {

    @Test
    @DisplayName("Given 사용자 When updatePassword Then 비밀번호 업데이트됨")
    void givenUser_whenUpdatePassword_thenPasswordUpdated() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.updatePassword("testuser", "new-encoded-password");

      // Then
      assertThat(account.getPassword()).isEqualTo("new-encoded-password");
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("incrementFailedAttempt 메서드")
  class IncrementFailedAttemptTests {

    @Test
    @DisplayName("Given 사용자 When incrementFailedAttempt Then 실패 횟수 증가")
    void givenUser_whenIncrement_thenAttemptIncreased() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.incrementFailedAttempt("testuser");

      // Then
      assertThat(account.getFailedLoginAttempts()).isEqualTo(1);
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("resetFailedAttempts 메서드")
  class ResetFailedAttemptsTests {

    @Test
    @DisplayName("Given 실패 횟수 있음 When resetFailedAttempts Then 0으로 초기화")
    void givenFailedAttempts_whenReset_thenZero() {
      // Given
      UserAccount account = createUserAccount("testuser");
      account.incrementFailedAttempt();
      account.incrementFailedAttempt();
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.resetFailedAttempts("testuser");

      // Then
      assertThat(account.getFailedLoginAttempts()).isZero();
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("lockUntil 메서드")
  class LockUntilTests {

    @Test
    @DisplayName("Given 사용자와 잠금 시간 When lockUntil Then 계정 잠금")
    void givenUserAndTime_whenLock_thenAccountLocked() {
      // Given
      UserAccount account = createUserAccount("testuser");
      Instant lockTime = Instant.now().plusSeconds(3600);
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.lockUntil("testuser", lockTime);

      // Then
      assertThat(account.getLockedUntil()).isEqualTo(lockTime);
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("activate/deactivate 메서드")
  class ActivateDeactivateTests {

    @Test
    @DisplayName("Given 비활성 사용자 When activate Then 활성화")
    void givenInactiveUser_whenActivate_thenActivated() {
      // Given
      UserAccount account = createUserAccount("testuser");
      account.deactivate();
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.activate("testuser");

      // Then
      assertThat(account.isActive()).isTrue();
      verify(repository).save(account);
    }

    @Test
    @DisplayName("Given 활성 사용자 When deactivate Then 비활성화")
    void givenActiveUser_whenDeactivate_thenDeactivated() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.deactivate("testuser");

      // Then
      assertThat(account.isActive()).isFalse();
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("getByIdOrThrow 메서드")
  class GetByIdOrThrowTests {

    @Test
    @DisplayName("Given 존재하는 ID When getByIdOrThrow Then 사용자 반환")
    void givenExistingId_whenGet_thenReturnUser() {
      // Given
      UUID id = UUID.randomUUID();
      UserAccount account = createUserAccount("testuser");
      when(repository.findById(id)).thenReturn(Optional.of(account));

      // When
      UserAccount result = service.getByIdOrThrow(id);

      // Then
      assertThat(result).isEqualTo(account);
    }

    @Test
    @DisplayName("Given 존재하지 않는 ID When getByIdOrThrow Then UserNotFoundException 발생")
    void givenNonExistingId_whenGet_thenThrowException() {
      // Given
      UUID id = UUID.randomUUID();
      when(repository.findById(id)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.getByIdOrThrow(id))
          .isInstanceOf(UserNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("existsByUsername 메서드")
  class ExistsByUsernameTests {

    @Test
    @DisplayName("Given 존재하는 사용자명 When existsByUsername Then true")
    void givenExistingUsername_whenCheck_thenTrue() {
      // Given
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(createUserAccount("testuser")));

      // When
      boolean result = service.existsByUsername("testuser");

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 존재하지 않는 사용자명 When existsByUsername Then false")
    void givenNonExistingUsername_whenCheck_thenFalse() {
      // Given
      when(repository.findByUsername("nonexistent")).thenReturn(Optional.empty());

      // When
      boolean result = service.existsByUsername("nonexistent");

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("encodePassword 메서드")
  class EncodePasswordTests {

    @Test
    @DisplayName("Given 평문 비밀번호 When encodePassword Then 인코딩된 비밀번호 반환")
    void givenRawPassword_whenEncode_thenReturnEncoded() {
      // Given
      when(passwordEncoder.encode("rawPassword")).thenReturn("encoded-password");

      // When
      String result = service.encodePassword("rawPassword");

      // Then
      assertThat(result).isEqualTo("encoded-password");
    }
  }

  @Nested
  @DisplayName("unlock 메서드")
  class UnlockTests {

    @Test
    @DisplayName("Given 잠긴 계정 When unlock Then 잠금 해제")
    void givenLockedAccount_whenUnlock_thenUnlocked() {
      // Given
      UserAccount account = createUserAccount("testuser");
      account.lockUntil(Instant.now().plusSeconds(3600));
      account.incrementFailedAttempt();
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.unlock("testuser");

      // Then
      assertThat(account.getLockedUntil()).isNull();
      assertThat(account.getFailedLoginAttempts()).isZero();
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("updateLastLoginAt 메서드")
  class UpdateLastLoginAtTests {

    @Test
    @DisplayName("Given 사용자 When updateLastLoginAt Then 로그인 시간 업데이트")
    void givenUser_whenUpdate_thenLastLoginAtUpdated() {
      // Given
      UserAccount account = createUserAccount("testuser");
      when(repository.findByUsername("testuser")).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);
      Instant before = Instant.now().minusSeconds(1);

      // When
      service.updateLastLoginAt("testuser");

      // Then
      assertThat(account.getLastLoginAt()).isAfter(before);
      verify(repository).save(account);
    }
  }

  @Nested
  @DisplayName("softDelete 메서드")
  class SoftDeleteTests {

    @Test
    @DisplayName("Given ID When softDelete Then 계정 비활성화")
    void givenId_whenSoftDelete_thenDeactivated() {
      // Given
      UUID id = UUID.randomUUID();
      UserAccount account = createUserAccount("testuser");
      when(repository.findById(id)).thenReturn(Optional.of(account));
      when(repository.save(any(UserAccount.class))).thenReturn(account);

      // When
      service.softDelete(id);

      // Then
      assertThat(account.isActive()).isFalse();
      verify(repository).save(account);
    }
  }

  private UserAccount createUserAccount(String username) {
    return UserAccount.builder()
        .username(username)
        .password("encoded-password")
        .organizationCode("ORG001")
        .build();
  }
}
