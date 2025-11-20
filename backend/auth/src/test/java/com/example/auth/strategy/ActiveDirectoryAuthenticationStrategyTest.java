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
@DisplayName("ActiveDirectoryAuthenticationStrategy 테스트")
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
    @DisplayName("Given 유효한 AD 사용자 When authenticate 호출 Then UserAccount를 반환한다")
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
    @DisplayName("Given 잘못된 AD 자격 When authenticate 호출 Then InvalidCredentialsException을 던진다")
    void givenInvalidCredentialsWhenAuthenticateThenThrow() {
        given(activeDirectoryClient.authenticate("ad-user", "bad")).willReturn(false);

        assertThatThrownBy(() -> strategy.authenticate(new LoginRequest(LoginType.AD, "ad-user", "bad", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
