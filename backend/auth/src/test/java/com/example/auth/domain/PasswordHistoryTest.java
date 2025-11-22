package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordHistoryTest {

    @Test
    @DisplayName("비밀번호 히스토리는 생성 시 사용자, 해시, 변경 시간을 보존한다")
    void passwordHistoryStoresFields() {
        UserAccount user = UserAccount.builder()
                .username("user")
                .password("hash")
                .organizationCode("ORG")
                .permissionGroupCode("PG")
                .build();
        Instant before = Instant.now();

        PasswordHistory history = new PasswordHistory(user, "hash2");

        assertThat(history.getUser()).isEqualTo(user);
        assertThat(history.getPasswordHash()).isEqualTo("hash2");
        assertThat(history.getChangedAt()).isAfterOrEqualTo(before);
    }
}
