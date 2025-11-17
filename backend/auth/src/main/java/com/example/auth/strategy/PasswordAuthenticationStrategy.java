package com.example.auth.strategy;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;
import com.example.auth.security.AccountStatusPolicy;
import com.example.auth.security.PasswordPolicyValidator;

@Component
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {

    private final UserAccountService userAccountService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AccountStatusPolicy accountStatusPolicy;

    public PasswordAuthenticationStrategy(UserAccountService userAccountService,
                                          PasswordEncoder passwordEncoder,
                                          PasswordPolicyValidator passwordPolicyValidator,
                                          AccountStatusPolicy accountStatusPolicy) {
        this.userAccountService = userAccountService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.accountStatusPolicy = accountStatusPolicy;
    }

    @Override
    public LoginType supportedType() {
        return LoginType.PASSWORD;
    }

    @Override
    public UserAccount authenticate(LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            throw new InvalidCredentialsException();
        }
        passwordPolicyValidator.validate(request.password());
        UserAccount account = userAccountService.getByUsernameOrThrow(request.username());
        accountStatusPolicy.ensureLoginAllowed(account);
        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            accountStatusPolicy.onFailedLogin(account);
        }
        accountStatusPolicy.onSuccessfulLogin(account);
        return account;
    }
}
