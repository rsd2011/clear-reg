package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserAccountDomainTest {

  @Test
  void linkSsoIdAllowsSingleAssignmentAndRejectsOverwrite() {
    UserAccount account = UserAccount.builder()
        .username("u1")
        .password("p")
        .roles(Set.of("ROLE_USER"))
        .organizationCode("ORG")
        .permissionGroupCode("DEFAULT")
        .build();

    account.linkSsoId("sso-1");
    assertThat(account.getSsoId()).isEqualTo("sso-1");

    assertThatThrownBy(() -> account.linkSsoId("other"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void assignActiveDirectoryDomainSetsValue() {
    UserAccount account = UserAccount.builder()
        .username("u2")
        .password("p")
        .roles(Set.of("ROLE_USER"))
        .organizationCode("ORG")
        .permissionGroupCode("DEFAULT")
        .build();

    account.assignActiveDirectoryDomain("corp.example.com");
    assertThat(account.getActiveDirectoryDomain()).isEqualTo("corp.example.com");
  }

  @Test
  void lockAndActivateTransitions() {
    UserAccount account = UserAccount.builder()
        .username("u3")
        .password("p")
        .roles(Set.of("ROLE_USER"))
        .organizationCode("ORG")
        .permissionGroupCode("DEFAULT")
        .build();

    account.lockUntil(Instant.now().plusSeconds(60));
    assertThat(account.isLocked()).isTrue();

    account.activate();
    assertThat(account.isLocked()).isFalse();
    assertThat(account.isActive()).isTrue();
  }
}
