package com.example.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.dto.JitProvisioningResult;
import com.example.admin.user.service.JitProvisioningService;
import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.ad.ActiveDirectoryClient;
import com.example.auth.dto.LoginRequest;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ActiveDirectoryAuthenticationStrategy 테스트")
@ExtendWith(MockitoExtension.class)
class ActiveDirectoryAuthenticationStrategyTest {

  @Mock private ActiveDirectoryClient activeDirectoryClient;
  @Mock private UserAccountProvider userAccountProvider;
  @Mock private JitProvisioningService jitProvisioningService;

  private ActiveDirectoryAuthenticationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy =
        new ActiveDirectoryAuthenticationStrategy(
            activeDirectoryClient, userAccountProvider, jitProvisioningService);
  }

  @Test
  @DisplayName("supportedType은 AD를 반환해야 함")
  void supportedType_returnsAd() {
    assertThat(strategy.supportedType()).isEqualTo(LoginType.AD);
  }

  @Test
  @DisplayName("Given null username When authenticate Then InvalidCredentialsException 발생")
  void givenNullUsername_whenAuthenticate_thenThrowException() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.AD, null, "password", null);

    // When & Then
    assertThatThrownBy(() -> strategy.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given null password When authenticate Then InvalidCredentialsException 발생")
  void givenNullPassword_whenAuthenticate_thenThrowException() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.AD, "testuser", null, null);

    // When & Then
    assertThatThrownBy(() -> strategy.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given AD 인증 실패 When authenticate Then InvalidCredentialsException 발생")
  void givenAdAuthFailed_whenAuthenticate_thenThrowException() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.AD, "testuser", "wrongpassword", null);
    when(activeDirectoryClient.authenticate("testuser", "wrongpassword")).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> strategy.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given 유효한 자격증명, JIT 비활성화 When authenticate Then 기존 사용자 조회")
  void givenValidCredentialsWithoutJit_whenAuthenticate_thenLookupExistingUser() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.AD, "testuser", "validpassword", null);
    UserAccountInfo account = createAccountInfo("testuser");

    when(activeDirectoryClient.authenticate("testuser", "validpassword")).thenReturn(true);
    when(jitProvisioningService.isEnabledFor("AD")).thenReturn(false);
    when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(account);

    // When
    UserAccountInfo result = strategy.authenticate(request);

    // Then
    assertThat(result).isEqualTo(account);
  }

  @Test
  @DisplayName("Given 유효한 자격증명, JIT 활성화 When authenticate Then JIT 프로비저닝 수행")
  void givenValidCredentialsWithJit_whenAuthenticate_thenPerformJitProvisioning() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.AD, "testuser", "validpassword", null);
    UserAccount userAccount =
        UserAccount.builder()
            .username("testuser")
            .password("encoded")
            .organizationCode("ORG001")
            .build();
    JitProvisioningResult jitResult = JitProvisioningResult.existing(userAccount);

    when(activeDirectoryClient.authenticate("testuser", "validpassword")).thenReturn(true);
    when(jitProvisioningService.isEnabledFor("AD")).thenReturn(true);
    when(activeDirectoryClient.getDomain()).thenReturn("CORP.LOCAL");
    when(jitProvisioningService.provisionForAd("testuser", "CORP.LOCAL")).thenReturn(jitResult);

    // When
    UserAccountInfo result = strategy.authenticate(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("testuser");
  }

  private UserAccountInfo createAccountInfo(String username) {
    return new UserAccountInfo() {
      @Override
      public UUID getId() {
        return UUID.randomUUID();
      }

      @Override
      public String getUsername() {
        return username;
      }

      @Override
      public String getPassword() {
        return "encoded";
      }

      @Override
      public String getEmail() {
        return username + "@example.com";
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
        return Instant.now();
      }
    };
  }
}
