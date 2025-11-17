package com.example.auth.strategy;

import org.springframework.stereotype.Component;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.ad.ActiveDirectoryClient;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;

@Component
public class ActiveDirectoryAuthenticationStrategy implements AuthenticationStrategy {

    private final ActiveDirectoryClient activeDirectoryClient;
    private final UserAccountService userAccountService;

    public ActiveDirectoryAuthenticationStrategy(ActiveDirectoryClient activeDirectoryClient,
                                                 UserAccountService userAccountService) {
        this.activeDirectoryClient = activeDirectoryClient;
        this.userAccountService = userAccountService;
    }

    @Override
    public LoginType supportedType() {
        return LoginType.AD;
    }

    @Override
    public UserAccount authenticate(LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            throw new InvalidCredentialsException();
        }
        if (!activeDirectoryClient.authenticate(request.username(), request.password())) {
            throw new InvalidCredentialsException();
        }
        return userAccountService.getByUsernameOrThrow(request.username());
    }
}
