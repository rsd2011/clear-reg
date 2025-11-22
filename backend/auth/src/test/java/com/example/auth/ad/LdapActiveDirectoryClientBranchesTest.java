package com.example.auth.ad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ldap.core.LdapTemplate;

class LdapActiveDirectoryClientBranchesTest {

    @Test
    @DisplayName("LDAP 템플릿이 예외를 던지면 false를 반환한다")
    void returnsFalseOnLdapException() {
        LdapTemplate template = mock(LdapTemplate.class);
        given(template.authenticate("", "(sAMAccountName=user)", "pw")).willThrow(new RuntimeException("ldap down"));

        LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.of(template));

        assertThat(client.authenticate("user", "pw")).isFalse();
    }

    @Test
    @DisplayName("템플릿이 없으면 fallback으로 ad-password만 허용한다")
    void fallbackAcceptsOnlyAdPassword() {
        LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.empty());

        assertThat(client.authenticate("user", "wrong")).isFalse();
        assertThat(client.authenticate("user", "ad-password")).isTrue();
    }
}
