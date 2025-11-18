package com.example.auth.domain;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.security.PasswordHistoryService;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;

    public UserAccountService(UserAccountRepository repository,
                              PasswordEncoder passwordEncoder,
                              PasswordHistoryService passwordHistoryService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryService = passwordHistoryService;
    }

    public UserAccount getByUsernameOrThrow(String username) {
        return repository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
    }

    public Optional<UserAccount> findBySsoId(String ssoId) {
        return repository.findBySsoId(ssoId);
    }

    @Transactional
    public UserAccount save(UserAccount account) {
        return repository.save(account);
    }

    @Transactional
    public void changePassword(UserAccount account, String rawPassword) {
        passwordHistoryService.ensureNotReused(account, rawPassword);
        String encoded = passwordEncoder.encode(rawPassword);
        account.updatePassword(encoded);
        repository.save(account);
        passwordHistoryService.record(account, encoded);
    }

    public boolean passwordMatches(UserAccount account, String rawPassword) {
        return passwordEncoder.matches(rawPassword, account.getPassword());
    }
}
