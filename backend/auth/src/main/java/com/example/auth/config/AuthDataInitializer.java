package com.example.auth.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;

@Component
@Profile("!test")
public class AuthDataInitializer implements CommandLineRunner {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        UserAccount passwordUser = UserAccount.builder()
                .username("local-user")
                .password(passwordEncoder.encode("local-password"))
                .email("local@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();

        UserAccount adUser = UserAccount.builder()
                .username("ad-user")
                .password(passwordEncoder.encode("unused"))
                .email("ad@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
        adUser.setActiveDirectoryDomain("corp.example.com");

        UserAccount ssoUser = UserAccount.builder()
                .username("sso-user")
                .password(passwordEncoder.encode("unused"))
                .email("sso@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
        ssoUser.setSsoId("sso-sso-user");

        repository.save(passwordUser);
        repository.save(adUser);
        repository.save(ssoUser);
    }
}
