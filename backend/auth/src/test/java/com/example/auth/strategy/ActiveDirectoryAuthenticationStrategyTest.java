package com.example.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.ad.ActiveDirectoryClient;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActiveDirectoryAuthenticationStrategy")
class ActiveDirectoryAuthenticationStrategyTest {

    @Mock
    private ActiveDirectoryClient activeDirectoryClient;

    @Mock
    private UserAccountService userAccountService;

    private ActiveDirectoryAuthenticationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ActiveDirectoryAuthenticationStrategy(activeDirectoryClient, userAccountService);
    }

    @Test
    @DisplayName("Given valid AD user When authenticate Then return account")
    void givenValidUserWhenAuthenticateThenReturnAccount() {
        var account = UserAccount.builder()
                .username("ad-user")
                .password("ignored")
                .email("ad@example.com")
                .build();
        given(activeDirectoryClient.authenticate("ad-user", "secret")).willReturn(true);
        given(userAccountService.getByUsernameOrThrow("ad-user")).willReturn(account);

        UserAccount result = strategy.authenticate(new LoginRequest(LoginType.AD, "ad-user", "secret", null));

        assertThat(result).isEqualTo(account);
    }

    @Test
    @DisplayName("Given invalid AD credentials When authenticate Then throw exception")
    void givenInvalidCredentialsWhenAuthenticateThenThrow() {
        given(activeDirectoryClient.authenticate("ad-user", "bad")).willReturn(false);

        assertThatThrownBy(() -> strategy.authenticate(new LoginRequest(LoginType.AD, "ad-user", "bad", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
