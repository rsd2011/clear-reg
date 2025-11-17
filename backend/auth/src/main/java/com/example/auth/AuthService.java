package com.example.auth;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.auth.domain.RefreshTokenService;
import com.example.auth.domain.RefreshTokenService.IssuedRefreshToken;
import com.example.auth.domain.UserAccount;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.TokenResponse;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.JwtTokenProvider.JwtToken;
import com.example.auth.strategy.AuthenticationStrategy;

@Service
public class AuthService {

    private final Map<LoginType, AuthenticationStrategy> strategies;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(List<AuthenticationStrategy> strategies,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService) {
        this.strategies = new EnumMap<>(LoginType.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.supportedType(), strategy));
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        AuthenticationStrategy strategy = strategies.get(request.type());
        if (strategy == null) {
            throw new InvalidCredentialsException();
        }
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

    private LoginResponse assembleResponse(UserAccount account, LoginType type, IssuedRefreshToken refreshToken) {
        TokenResponse tokenResponse = buildTokenResponse(account, refreshToken);
        return new LoginResponse(account.getUsername(), type, tokenResponse);
    }

    private TokenResponse buildTokenResponse(UserAccount account, IssuedRefreshToken refreshToken) {
        JwtToken accessToken = jwtTokenProvider.generateAccessToken(account.getUsername(), account.getRoles());
        return new TokenResponse(accessToken.value(), accessToken.expiresAt(), refreshToken.value(), refreshToken.expiresAt());
    }
}
