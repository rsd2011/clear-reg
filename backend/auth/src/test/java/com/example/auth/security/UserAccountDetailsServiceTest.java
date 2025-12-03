package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@DisplayName("UserAccountDetailsService 테스트")
@ExtendWith(MockitoExtension.class)
class UserAccountDetailsServiceTest {

  @Mock private UserAccountProvider userAccountProvider;

  private UserAccountDetailsService service;

  @BeforeEach
  void setUp() {
    service = new UserAccountDetailsService(userAccountProvider);
  }

  @Test
  @DisplayName("Given 존재하는 사용자 When loadUserByUsername Then UserDetails 반환")
  void givenExistingUser_whenLoadByUsername_thenReturnUserDetails() {
    // Given
    UserAccountInfo account = createAccountInfo("testuser", "encoded-password", Set.of("ROLE_USER", "ROLE_ADMIN"));
    when(userAccountProvider.findByUsername("testuser")).thenReturn(Optional.of(account));

    // When
    UserDetails result = service.loadUserByUsername("testuser");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("testuser");
    assertThat(result.getPassword()).isEqualTo("encoded-password");
    assertThat(result.getAuthorities()).hasSize(2);
  }

  @Test
  @DisplayName("Given 존재하지 않는 사용자 When loadUserByUsername Then UsernameNotFoundException 발생")
  void givenNonExistingUser_whenLoadByUsername_thenThrowException() {
    // Given
    when(userAccountProvider.findByUsername("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> service.loadUserByUsername("nonexistent"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("User not found: nonexistent");
  }

  @Test
  @DisplayName("Given 권한 없는 사용자 When loadUserByUsername Then 빈 권한으로 반환")
  void givenUserWithNoRoles_whenLoadByUsername_thenReturnEmptyAuthorities() {
    // Given
    UserAccountInfo account = createAccountInfo("testuser", "password", Set.of());
    when(userAccountProvider.findByUsername("testuser")).thenReturn(Optional.of(account));

    // When
    UserDetails result = service.loadUserByUsername("testuser");

    // Then
    assertThat(result.getAuthorities()).isEmpty();
  }

  private UserAccountInfo createAccountInfo(String username, String password, Set<String> roles) {
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
        return password;
      }

      @Override
      public String getEmail() {
        return username + "@example.com";
      }

      @Override
      public Set<String> getRoles() {
        return roles;
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
