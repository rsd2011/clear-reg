package com.example.auth.ad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.LdapTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("LdapActiveDirectoryClient 테스트")
class LdapActiveDirectoryClientTest {

  @Mock private LdapTemplate ldapTemplate;

  @Test
  @DisplayName("Given LDAP 템플릿 When authenticate 호출 Then 템플릿으로 위임한다")
  void givenTemplateWhenAuthenticateThenDelegate() {
    given(ldapTemplate.authenticate(anyString(), anyString(), anyString())).willReturn(true);
    LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.of(ldapTemplate));

    assertThat(client.authenticate("user", "pw")).isTrue();
  }

  @Test
  @DisplayName("Given 템플릿이 없을 때 When authenticate 호출 Then 폴백 인증을 사용한다")
  void givenNoTemplateWhenAuthenticateThenFallback() {
    LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.empty());

    assertThat(client.authenticate("user", "ad-password")).isTrue();
    assertThat(client.authenticate("user", "nope")).isFalse();
  }

  @Test
  @DisplayName("Given LDAP에서 예외 발생 When authenticate Then false 반환")
  void givenExceptionWhenAuthenticateThenReturnFalse() {
    given(ldapTemplate.authenticate(anyString(), anyString(), anyString()))
            .willThrow(new RuntimeException("LDAP connection error"));
    LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.of(ldapTemplate));

    assertThat(client.authenticate("user", "pw")).isFalse();
  }

  @Test
  @DisplayName("Given 폴백모드에서 null username When authenticate Then false 반환")
  void givenNullUsernameInFallbackThenReturnFalse() {
    LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.empty());

    assertThat(client.authenticate(null, "ad-password")).isFalse();
  }

  @Test
  @DisplayName("Given 폴백모드에서 null password When authenticate Then false 반환")
  void givenNullPasswordInFallbackThenReturnFalse() {
    LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.empty());

    assertThat(client.authenticate("user", null)).isFalse();
  }

  @Test
  @DisplayName("getDomain default 메서드는 DEFAULT를 반환한다")
  void getDomain_returnsDefault() {
    ActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.empty());

    assertThat(client.getDomain()).isEqualTo("DEFAULT");
  }
}
