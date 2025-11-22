package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.dto.LoginRequest;
import com.example.auth.strategy.SsoAuthenticationStrategy;
import com.example.auth.sso.SsoClient;
import com.example.auth.domain.UserAccountService;

@ExtendWith(MockitoExtension.class)
class SsoAuthenticationStrategyNullUsernameTest {

    @Mock SsoClient ssoClient;
    @Mock UserAccountService userAccountService;

    @Test
    @DisplayName("SSO 토큰에서 사용자명을 해석하지 못하면 인증에 실패한다")
    void failWhenUsernameNull() {
        SsoAuthenticationStrategy strategy = new SsoAuthenticationStrategy(ssoClient, userAccountService);
        given(ssoClient.resolveUsername("token")).willReturn(null);
        given(userAccountService.getByUsernameOrThrow(null)).willThrow(new InvalidCredentialsException());

        LoginRequest request = new LoginRequest(LoginType.SSO, null, null, "token");

        assertThatThrownBy(() -> strategy.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
