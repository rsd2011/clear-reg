package com.example.auth.ad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LdapActiveDirectoryClientNullTest {

    @Test
    @DisplayName("username이나 password가 null이면 fallback에서도 실패한다")
    void nullCredentialsFail() {
        LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.empty());
        assertThat(client.authenticate(null, "ad-password")).isFalse();
        assertThat(client.authenticate("user", null)).isFalse();
    }
}
