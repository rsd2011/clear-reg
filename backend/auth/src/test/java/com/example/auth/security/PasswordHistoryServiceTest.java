package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.repository.UserAccountRepository;
import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.PasswordHistory;
import com.example.auth.domain.PasswordHistoryRepository;
import com.example.common.user.spi.UserAccountInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("PasswordHistoryService 테스트")
@ExtendWith(MockitoExtension.class)
class PasswordHistoryServiceTest {

  @Mock private PasswordHistoryRepository repository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthPolicyProperties properties;
  @Mock private PolicyToggleProvider policyToggleProvider;

  private PasswordHistoryService service;

  @BeforeEach
  void setUp() {
    service =
        new PasswordHistoryService(
            repository, userAccountRepository, passwordEncoder, properties, policyToggleProvider);
  }

  @Nested
  @DisplayName("record 메서드")
  class RecordTests {

    @Test
    @DisplayName("Given 히스토리 비활성화 When record Then 아무 작업 안함")
    void givenHistoryDisabled_whenRecord_thenDoNothing() {
      // Given
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(false);

      // When
      service.record("testuser", "encoded-password");

      // Then
      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Given 히스토리 활성화 When record Then 히스토리 저장")
    void givenHistoryEnabled_whenRecord_thenSaveHistory() {
      // Given
      UserAccount user = createUserAccount("testuser");
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
      when(properties.getPasswordHistorySize()).thenReturn(5);
      when(repository.findByUserUsernameOrderByChangedAtDesc("testuser"))
          .thenReturn(new ArrayList<>());

      // When
      service.record("testuser", "encoded-password");

      // Then
      ArgumentCaptor<PasswordHistory> captor = ArgumentCaptor.forClass(PasswordHistory.class);
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    @DisplayName("Given 사용자 없음 When record Then InvalidCredentialsException 발생")
    void givenUserNotFound_whenRecord_thenThrowException() {
      // Given
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.record("nonexistent", "encoded-password"))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 히스토리 초과 When record Then 오래된 히스토리 삭제")
    void givenExcessHistory_whenRecord_thenPruneOld() {
      // Given
      UserAccount user = createUserAccount("testuser");
      List<PasswordHistory> histories = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        histories.add(new PasswordHistory(user, "hash-" + i));
      }

      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
      when(properties.getPasswordHistorySize()).thenReturn(5);
      when(repository.findByUserUsernameOrderByChangedAtDesc("testuser")).thenReturn(histories);

      // When
      service.record("testuser", "new-encoded-password");

      // Then
      verify(repository, times(2)).delete(any(PasswordHistory.class)); // 7-5 = 2개 삭제
    }
  }

  @Nested
  @DisplayName("ensureNotReused 메서드")
  class EnsureNotReusedTests {

    @Test
    @DisplayName("Given 히스토리 비활성화 When ensureNotReused Then 통과")
    void givenHistoryDisabled_whenEnsure_thenPass() {
      // Given
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(false);

      // When & Then - no exception
      service.ensureNotReused("testuser", "any-password");
      verify(repository, never()).findByUserUsernameOrderByChangedAtDesc(any());
    }

    @Test
    @DisplayName("Given 중복 없음 When ensureNotReused Then 통과")
    void givenNoDuplicate_whenEnsure_thenPass() {
      // Given
      UserAccount user = createUserAccount("testuser");
      List<PasswordHistory> histories = List.of(new PasswordHistory(user, "old-hash"));

      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(properties.getPasswordHistorySize()).thenReturn(5);
      when(repository.findByUserUsernameOrderByChangedAtDesc("testuser")).thenReturn(histories);
      when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);

      // When & Then - no exception
      service.ensureNotReused("testuser", "new-password");
    }

    @Test
    @DisplayName("Given 중복 존재 When ensureNotReused Then InvalidCredentialsException 발생")
    void givenDuplicate_whenEnsure_thenThrowException() {
      // Given
      UserAccount user = createUserAccount("testuser");
      List<PasswordHistory> histories = List.of(new PasswordHistory(user, "reused-hash"));

      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(properties.getPasswordHistorySize()).thenReturn(5);
      when(repository.findByUserUsernameOrderByChangedAtDesc("testuser")).thenReturn(histories);
      when(passwordEncoder.matches("reused-password", "reused-hash")).thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> service.ensureNotReused("testuser", "reused-password"))
          .isInstanceOf(InvalidCredentialsException.class);
    }
  }

  @Nested
  @DisplayName("isExpired 메서드")
  class IsExpiredTests {

    @Test
    @DisplayName("Given 히스토리 비활성화 When isExpired Then false")
    void givenHistoryDisabled_whenIsExpired_thenFalse() {
      // Given
      UserAccountInfo account = createAccountInfo(Instant.now().minusSeconds(86400 * 100));
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(false);

      // When
      boolean result = service.isExpired(account);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given 만료일 0 이하 When isExpired Then false")
    void givenZeroExpiryDays_whenIsExpired_thenFalse() {
      // Given
      UserAccountInfo account = createAccountInfo(Instant.now().minusSeconds(86400 * 100));
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(properties.getPasswordExpiryDays()).thenReturn(0L);

      // When
      boolean result = service.isExpired(account);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given passwordChangedAt null When isExpired Then true")
    void givenNullPasswordChangedAt_whenIsExpired_thenTrue() {
      // Given
      UserAccountInfo account = createAccountInfo(null);
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(properties.getPasswordExpiryDays()).thenReturn(90L);

      // When
      boolean result = service.isExpired(account);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 만료 기한 내 When isExpired Then false")
    void givenWithinExpiry_whenIsExpired_thenFalse() {
      // Given
      UserAccountInfo account = createAccountInfo(Instant.now().minusSeconds(86400 * 30)); // 30일 전
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(properties.getPasswordExpiryDays()).thenReturn(90L);

      // When
      boolean result = service.isExpired(account);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given 만료 기한 초과 When isExpired Then true")
    void givenPastExpiry_whenIsExpired_thenTrue() {
      // Given
      UserAccountInfo account =
          createAccountInfo(Instant.now().minusSeconds(86400 * 100)); // 100일 전
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(properties.getPasswordExpiryDays()).thenReturn(90L);

      // When
      boolean result = service.isExpired(account);

      // Then
      assertThat(result).isTrue();
    }
  }

  private UserAccount createUserAccount(String username) {
    return UserAccount.builder()
        .username(username)
        .password("encoded-password")
        .organizationCode("ORG001")
        .build();
  }

  private UserAccountInfo createAccountInfo(Instant passwordChangedAt) {
    return new UserAccountInfo() {
      @Override
      public UUID getId() {
        return UUID.randomUUID();
      }

      @Override
      public String getUsername() {
        return "testuser";
      }

      @Override
      public String getPassword() {
        return "encoded";
      }

      @Override
      public String getEmail() {
        return "test@example.com";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("ROLE_USER");
      }

      @Override
      public String getOrganizationCode() {
        return "ORG001";
      }

      @Override
      public String getPermissionGroupCode() {
        return "DEFAULT";
      }

      @Override
      public String getSsoId() {
        return null;
      }

      @Override
      public String getActiveDirectoryDomain() {
        return null;
      }

      @Override
      public boolean isActive() {
        return true;
      }

      @Override
      public boolean isLocked() {
        return false;
      }

      @Override
      public int getFailedLoginAttempts() {
        return 0;
      }

      @Override
      public Instant getLockedUntil() {
        return null;
      }

      @Override
      public Instant getPasswordChangedAt() {
        return passwordChangedAt;
      }
    };
  }
}
