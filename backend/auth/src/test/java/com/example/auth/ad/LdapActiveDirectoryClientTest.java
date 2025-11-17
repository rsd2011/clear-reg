package com.example.auth.ad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("LdapActiveDirectoryClient")
class LdapActiveDirectoryClientTest {

    @Mock
    private LdapTemplate ldapTemplate;

    @Test
    @DisplayName("Given LDAP template When authenticate Then delegate to template")
    void givenTemplateWhenAuthenticateThenDelegate() {
        given(ldapTemplate.authenticate(anyString(), anyString(), anyString())).willReturn(true);
        LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.of(ldapTemplate));

        assertThat(client.authenticate("user", "pw")).isTrue();
    }

    @Test
    @DisplayName("Given no template When authenticate Then use fallback")
    void givenNoTemplateWhenAuthenticateThenFallback() {
        LdapActiveDirectoryClient client = new LdapActiveDirectoryClient(Optional.empty());

        assertThat(client.authenticate("user", "ad-password")).isTrue();
        assertThat(client.authenticate("user", "nope")).isFalse();
    }
}
