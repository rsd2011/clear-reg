package com.example.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.auth.LoginType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthenticationStrategyResolver 테스트")
class AuthenticationStrategyResolverTest {

  @Test
  @DisplayName("Given 등록된 전략 When resolve Then 해당 전략 반환")
  void givenRegisteredStrategy_whenResolve_thenReturnStrategy() {
    // Given
    AuthenticationStrategy passwordStrategy = mock(AuthenticationStrategy.class);
    AuthenticationStrategy ssoStrategy = mock(AuthenticationStrategy.class);

    when(passwordStrategy.supportedType()).thenReturn(LoginType.PASSWORD);
    when(ssoStrategy.supportedType()).thenReturn(LoginType.SSO);

    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(passwordStrategy, ssoStrategy));

    // When
    Optional<AuthenticationStrategy> result = resolver.resolve(LoginType.PASSWORD);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(passwordStrategy);
  }

  @Test
  @DisplayName("Given 미등록 전략 When resolve Then empty 반환")
  void givenUnregisteredStrategy_whenResolve_thenReturnEmpty() {
    // Given
    AuthenticationStrategy passwordStrategy = mock(AuthenticationStrategy.class);
    when(passwordStrategy.supportedType()).thenReturn(LoginType.PASSWORD);

    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(passwordStrategy));

    // When
    Optional<AuthenticationStrategy> result = resolver.resolve(LoginType.SSO);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Given null 타입 When resolve Then empty 반환")
  void givenNullType_whenResolve_thenReturnEmpty() {
    // Given
    AuthenticationStrategy passwordStrategy = mock(AuthenticationStrategy.class);
    when(passwordStrategy.supportedType()).thenReturn(LoginType.PASSWORD);

    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(passwordStrategy));

    // When
    Optional<AuthenticationStrategy> result = resolver.resolve(null);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Given 빈 전략 목록 When resolve Then empty 반환")
  void givenEmptyStrategies_whenResolve_thenReturnEmpty() {
    // Given
    AuthenticationStrategyResolver resolver = new AuthenticationStrategyResolver(List.of());

    // When
    Optional<AuthenticationStrategy> result = resolver.resolve(LoginType.PASSWORD);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Given 여러 전략 등록 When resolve Then 각 타입에 맞는 전략 반환")
  void givenMultipleStrategies_whenResolve_thenReturnCorrectStrategy() {
    // Given
    AuthenticationStrategy passwordStrategy = mock(AuthenticationStrategy.class);
    AuthenticationStrategy ssoStrategy = mock(AuthenticationStrategy.class);
    AuthenticationStrategy adStrategy = mock(AuthenticationStrategy.class);

    when(passwordStrategy.supportedType()).thenReturn(LoginType.PASSWORD);
    when(ssoStrategy.supportedType()).thenReturn(LoginType.SSO);
    when(adStrategy.supportedType()).thenReturn(LoginType.AD);

    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(passwordStrategy, ssoStrategy, adStrategy));

    // When & Then
    assertThat(resolver.resolve(LoginType.PASSWORD).get()).isEqualTo(passwordStrategy);
    assertThat(resolver.resolve(LoginType.SSO).get()).isEqualTo(ssoStrategy);
    assertThat(resolver.resolve(LoginType.AD).get()).isEqualTo(adStrategy);
  }
}
