package com.example.auth.domain;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auth.InvalidCredentialsException;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
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
}
