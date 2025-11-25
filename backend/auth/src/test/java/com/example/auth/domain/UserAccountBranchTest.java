package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAccountBranchTest {

  @Test
  @DisplayName("lockedUntil이 미래면 isLocked가 true, null이면 false")
  void isLockedBranches() {
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .roles(Set.of("ADMIN"))
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    user.lockUntil(Instant.now().plusSeconds(30));
    assertThat(user.isLocked()).isTrue();

    user.lockUntil(null);
    assertThat(user.isLocked()).isFalse();
  }

  @Test
  @DisplayName("updatePassword는 passwordChangedAt을 갱신한다")
  void updatePasswordUpdatesTimestamp() {
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();
    Instant before = user.getPasswordChangedAt();

    user.updatePassword("newPw");

    assertThat(user.getPassword()).isEqualTo("newPw");
    assertThat(user.getPasswordChangedAt()).isAfter(before);
  }

  @Test
  @DisplayName("updateEmail은 이메일을 변경한다")
  void updateEmailChanges() {
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    user.updateEmail("new@example.com");

    assertThat(user.getEmail()).isEqualTo("new@example.com");
  }
}
