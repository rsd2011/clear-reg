package com.example.auth;

import org.springframework.stereotype.Service;

import com.example.auth.domain.RefreshTokenService;
import com.example.auth.domain.RefreshTokenService.IssuedRefreshToken;
import com.example.auth.domain.UserAccountService;
import com.example.auth.domain.UserAccount;
import com.example.auth.dto.AccountStatusChangeRequest;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.PasswordChangeRequest;
import com.example.auth.dto.TokenResponse;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.JwtTokenProvider.JwtToken;
import com.example.auth.security.AccountStatusPolicy;
import com.example.auth.security.PasswordPolicyValidator;
import com.example.auth.security.PolicyToggleProvider;
import com.example.auth.strategy.AuthenticationStrategy;
import com.example.auth.strategy.AuthenticationStrategyResolver;

@Service
public class AuthService {

    private final AuthenticationStrategyResolver strategyResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserAccountService userAccountService;
    private final AccountStatusPolicy accountStatusPolicy;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PolicyToggleProvider policyToggleProvider;

    public AuthService(AuthenticationStrategyResolver strategyResolver,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService,
                       UserAccountService userAccountService,
                       AccountStatusPolicy accountStatusPolicy,
                       PasswordPolicyValidator passwordPolicyValidator,
                       PolicyToggleProvider policyToggleProvider) {
        this.strategyResolver = strategyResolver;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userAccountService = userAccountService;
        this.accountStatusPolicy = accountStatusPolicy;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.policyToggleProvider = policyToggleProvider;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.type() == null || !policyToggleProvider.enabledLoginTypes().contains(request.type())) {
            throw new InvalidCredentialsException();
        }
        AuthenticationStrategy strategy = strategyResolver.resolve(request.type())
                .orElseThrow(InvalidCredentialsException::new);
        UserAccount account = strategy.authenticate(request);
        return assembleResponse(account, request.type(), refreshTokenService.issue(account));
    }

    public TokenResponse refreshTokens(String refreshTokenValue) {
        IssuedRefreshToken issued = refreshTokenService.rotate(refreshTokenValue);
        return buildTokenResponse(issued.user(), issued);
    }

    public void logout(String refreshTokenValue) {
        refreshTokenService.revoke(refreshTokenValue);
    }

    public void changePassword(String username, PasswordChangeRequest request) {
        UserAccount account = userAccountService.getByUsernameOrThrow(username);
        accountStatusPolicy.ensureLoginAllowed(account);
        if (!userAccountService.passwordMatches(account, request.currentPassword())) {
            accountStatusPolicy.onFailedLogin(account);
            throw new InvalidCredentialsException();
        }
        passwordPolicyValidator.validate(request.newPassword());
        userAccountService.changePassword(account, request.newPassword());
        accountStatusPolicy.onSuccessfulLogin(account);
    }

    public void updateAccountStatus(AccountStatusChangeRequest request) {
        UserAccount account = userAccountService.getByUsernameOrThrow(request.username());
        if (request.active()) {
            accountStatusPolicy.activate(account);
        } else {
            accountStatusPolicy.deactivate(account);
            refreshTokenService.revokeAll(account);
        }
    }

    private LoginResponse assembleResponse(UserAccount account, LoginType type, IssuedRefreshToken refreshToken) {
        TokenResponse tokenResponse = buildTokenResponse(account, refreshToken);
        return new LoginResponse(account.getUsername(), type, tokenResponse);
    }

    private TokenResponse buildTokenResponse(UserAccount account, IssuedRefreshToken refreshToken) {
        JwtToken accessToken = jwtTokenProvider.generateAccessToken(account.getUsername(), account.getRoles());
        return new TokenResponse(accessToken.value(), accessToken.expiresAt(), refreshToken.value(), refreshToken.expiresAt());
    }
}
