package com.example.auth.ad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ldap.core.LdapTemplate;

class LdapActiveDirectoryClientFalseTest {

    @Test
    @DisplayName("LDAP 템플릿이 false를 반환하면 인증 실패")
    void authenticateReturnsFalse() {
        LdapTemplate template = org.mockito.Mockito.mock(LdapTemplate.class);
        given(template.authenticate("", "(sAMAccountName=user)", "pw")).willReturn(false);

        LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.of(template));

        assertThat(client.authenticate("user", "pw")).isFalse();
    }
}
