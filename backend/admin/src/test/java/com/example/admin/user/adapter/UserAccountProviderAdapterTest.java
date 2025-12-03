package com.example.admin.user.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.service.UserAccountService;
import com.example.common.user.spi.UserAccountInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserAccountProviderAdapter 테스트")
@ExtendWith(MockitoExtension.class)
class UserAccountProviderAdapterTest {

  @Mock private UserAccountService userAccountService;

  private UserAccountProviderAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new UserAccountProviderAdapter(userAccountService);
  }

  @Test
  @DisplayName("Given username When getByUsernameOrThrow Then UserAccountService 위임")
  void givenUsername_whenGetByUsernameOrThrow_thenDelegate() {
    // Given
    UserAccount account = createUserAccount("testuser");
    when(userAccountService.getByUsernameOrThrow("testuser")).thenReturn(account);

    // When
    UserAccount result = adapter.getByUsernameOrThrow("testuser");

    // Then
    assertThat(result).isEqualTo(account);
    verify(userAccountService).getByUsernameOrThrow("testuser");
  }

  @Test
  @DisplayName("Given username When findByUsername Then UserAccountService 위임")
  void givenUsername_whenFindByUsername_thenDelegate() {
    // Given
    UserAccount account = createUserAccount("testuser");
    when(userAccountService.findByUsername("testuser")).thenReturn(Optional.of(account));

    // When
    Optional<UserAccountInfo> result = adapter.findByUsername("testuser");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(account);
    verify(userAccountService).findByUsername("testuser");
  }

  @Test
  @DisplayName("Given ssoId When findBySsoId Then UserAccountService 위임")
  void givenSsoId_whenFindBySsoId_thenDelegate() {
    // Given
    UserAccount account = createUserAccount("testuser");
    when(userAccountService.findBySsoId("sso-id-123")).thenReturn(Optional.of(account));

    // When
    Optional<UserAccountInfo> result = adapter.findBySsoId("sso-id-123");

    // Then
    assertThat(result).isPresent();
    verify(userAccountService).findBySsoId("sso-id-123");
  }

  @Test
  @DisplayName("Given codes When findByPermissionGroupCodeIn Then UserAccountService 위임")
  void givenCodes_whenFindByPermissionGroupCodeIn_thenDelegate() {
    // Given
    List<String> codes = List.of("GROUP_A", "GROUP_B");
    List<UserAccount> accounts = List.of(createUserAccount("user1"), createUserAccount("user2"));
    when(userAccountService.findByPermissionGroupCodeIn(codes)).thenReturn(accounts);

    // When
    List<? extends UserAccountInfo> result = adapter.findByPermissionGroupCodeIn(codes);

    // Then
    assertThat(result).hasSize(2);
    verify(userAccountService).findByPermissionGroupCodeIn(codes);
  }

  @Test
  @DisplayName("Given username and password When passwordMatches Then UserAccountService 위임")
  void givenUsernameAndPassword_whenPasswordMatches_thenDelegate() {
    // Given
    UserAccount account = createUserAccount("testuser");
    when(userAccountService.getByUsernameOrThrow("testuser")).thenReturn(account);
    when(userAccountService.passwordMatches(account, "password")).thenReturn(true);

    // When
    boolean result = adapter.passwordMatches("testuser", "password");

    // Then
    assertThat(result).isTrue();
    verify(userAccountService).passwordMatches(account, "password");
  }

  @Test
  @DisplayName("Given username When incrementFailedAttempt Then UserAccountService 위임")
  void givenUsername_whenIncrementFailedAttempt_thenDelegate() {
    // When
    adapter.incrementFailedAttempt("testuser");

    // Then
    verify(userAccountService).incrementFailedAttempt("testuser");
  }

  @Test
  @DisplayName("Given username When resetFailedAttempts Then UserAccountService 위임")
  void givenUsername_whenResetFailedAttempts_thenDelegate() {
    // When
    adapter.resetFailedAttempts("testuser");

    // Then
    verify(userAccountService).resetFailedAttempts("testuser");
  }

  @Test
  @DisplayName("Given username and until When lockUntil Then UserAccountService 위임")
  void givenUsernameAndUntil_whenLockUntil_thenDelegate() {
    // Given
    Instant until = Instant.now().plusSeconds(3600);

    // When
    adapter.lockUntil("testuser", until);

    // Then
    verify(userAccountService).lockUntil("testuser", until);
  }

  @Test
  @DisplayName("Given username When activate Then UserAccountService 위임")
  void givenUsername_whenActivate_thenDelegate() {
    // When
    adapter.activate("testuser");

    // Then
    verify(userAccountService).activate("testuser");
  }

  @Test
  @DisplayName("Given username When deactivate Then UserAccountService 위임")
  void givenUsername_whenDeactivate_thenDelegate() {
    // When
    adapter.deactivate("testuser");

    // Then
    verify(userAccountService).deactivate("testuser");
  }

  @Test
  @DisplayName("Given username and password When updatePassword Then UserAccountService 위임")
  void givenUsernameAndPassword_whenUpdatePassword_thenDelegate() {
    // When
    adapter.updatePassword("testuser", "new-encoded-password");

    // Then
    verify(userAccountService).updatePassword("testuser", "new-encoded-password");
  }

  private UserAccount createUserAccount(String username) {
    return UserAccount.builder()
        .username(username)
        .password("encoded-password")
        .organizationCode("ORG001")
        .build();
  }
}
