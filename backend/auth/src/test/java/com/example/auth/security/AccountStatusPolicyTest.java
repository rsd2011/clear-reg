package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;

@DisplayName("AccountStatusPolicy")
class AccountStatusPolicyTest {

    @Mock
    private UserAccountRepository repository;

    private AccountStatusPolicy policy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AuthPolicyProperties properties = new AuthPolicyProperties();
        properties.setMaxFailedAttempts(1);
        properties.setLockoutSeconds(60);
        policy = new AccountStatusPolicy(properties, repository);
    }

    @Test
    @DisplayName("Given locked account When ensureLoginAllowed Then throw")
    void givenLockedAccountWhenEnsureThenThrow() {
        UserAccount account = UserAccount.builder()
                .username("locked")
                .password("pw")
                .email("locked@example.com")
                .build();
        account.lockUntil(Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given inactive account When ensureLoginAllowed Then throw")
    void givenInactiveAccountWhenEnsureThenThrow() {
        UserAccount account = UserAccount.builder()
                .username("inactive")
                .password("pw")
                .email("inactive@example.com")
                .build();
        account.deactivate();

        assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given failed login When onFailedLogin Then lock account")
    void givenFailedLoginWhenOnFailedLoginThenLock() {
        UserAccount account = UserAccount.builder()
                .username("user")
                .password("pw")
                .email("user@example.com")
                .build();

        assertThatThrownBy(() -> policy.onFailedLogin(account))
                .isInstanceOf(InvalidCredentialsException.class);
        then(repository).should().save(account);
    }

    @Test
    @DisplayName("Given locked account When onSuccessfulLogin Then unlock")
    void givenLockedAccountWhenSuccessThenUnlock() {
        UserAccount account = UserAccount.builder()
                .username("user")
                .password("pw")
                .email("user@example.com")
                .build();
        account.lockUntil(Instant.now().plusSeconds(60));
        account.incrementFailedAttempt();

        policy.onSuccessfulLogin(account);

        then(repository).should().save(account);
    }
}
