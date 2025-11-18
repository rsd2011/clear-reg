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
    private final PasswordHistoryService passwordHistoryService;
    private final PolicyToggleProvider policyToggleProvider;

    public AccountStatusPolicy(AuthPolicyProperties properties,
                               UserAccountRepository repository,
                               PasswordHistoryService passwordHistoryService,
                               PolicyToggleProvider policyToggleProvider) {
        this.properties = properties;
        this.repository = repository;
        this.passwordHistoryService = passwordHistoryService;
        this.policyToggleProvider = policyToggleProvider;
    }

    public void ensureLoginAllowed(UserAccount account) {
        if (!account.isActive()) {
            throw new InvalidCredentialsException();
        }
        if (policyToggleProvider.isAccountLockEnabled() && account.isLocked()) {
            throw new InvalidCredentialsException();
        }
        if (policyToggleProvider.isPasswordHistoryEnabled() && passwordHistoryService.isExpired(account)) {
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public void onSuccessfulLogin(UserAccount account) {
        if (!policyToggleProvider.isAccountLockEnabled()) {
            return;
        }
        if (account.getFailedLoginAttempts() > 0 || account.getLockedUntil() != null) {
            account.resetFailedAttempts();
            account.lockUntil(null);
            repository.save(account);
        }
    }

    @Transactional
    public void onFailedLogin(UserAccount account) {
        if (!policyToggleProvider.isAccountLockEnabled()) {
            throw new InvalidCredentialsException();
        }
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
