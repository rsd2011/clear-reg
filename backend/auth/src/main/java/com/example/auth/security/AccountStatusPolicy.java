package com.example.auth.security;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.auth.InvalidCredentialsException;

@Component
public class AccountStatusPolicy {

    private final AuthPolicyProperties properties;
    private final UserAccountRepository repository;

    public AccountStatusPolicy(AuthPolicyProperties properties, UserAccountRepository repository) {
        this.properties = properties;
        this.repository = repository;
    }

    public void ensureLoginAllowed(UserAccount account) {
        if (!account.isActive()) {
            throw new InvalidCredentialsException();
        }
        if (account.isLocked()) {
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public void onSuccessfulLogin(UserAccount account) {
        if (account.getFailedLoginAttempts() > 0 || account.getLockedUntil() != null) {
            account.resetFailedAttempts();
            account.lockUntil(null);
            repository.save(account);
        }
    }

    @Transactional
    public void onFailedLogin(UserAccount account) {
        account.incrementFailedAttempt();
        if (account.getFailedLoginAttempts() >= properties.getMaxFailedAttempts()) {
            account.lockUntil(Instant.now().plusSeconds(properties.getLockoutSeconds()));
            account.resetFailedAttempts();
        }
        repository.save(account);
        throw new InvalidCredentialsException();
    }

    @Transactional
    public void deactivate(UserAccount account) {
        account.deactivate();
        repository.save(account);
    }

    @Transactional
    public void activate(UserAccount account) {
        account.activate();
        repository.save(account);
    }
}
