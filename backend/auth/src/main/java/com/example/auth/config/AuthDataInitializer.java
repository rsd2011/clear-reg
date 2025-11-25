package com.example.auth.config;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.auth.domain.UserAccountService;
import com.example.auth.security.PasswordHistoryService;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AuthDataInitializer implements CommandLineRunner {

  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordHistoryService passwordHistoryService;
  private final UserAccountService userAccountService;

  public AuthDataInitializer(
      UserAccountRepository repository,
      PasswordEncoder passwordEncoder,
      PasswordHistoryService passwordHistoryService,
      UserAccountService userAccountService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.passwordHistoryService = passwordHistoryService;
    this.userAccountService = userAccountService;
  }

  @Override
  public void run(String... args) {
    if (repository.count() > 0) {
      return;
    }

    String defaultPassword = "local-password";
    UserAccount passwordUser =
        UserAccount.builder()
            .username("local-user")
            .password(passwordEncoder.encode(defaultPassword))
            .email("local@example.com")
            .roles(Set.of("ROLE_USER"))
            .organizationCode("ROOT")
            .permissionGroupCode("DEFAULT")
            .build();

    UserAccount adUser =
        UserAccount.builder()
            .username("ad-user")
            .password(passwordEncoder.encode("unused"))
            .email("ad@example.com")
            .roles(Set.of("ROLE_USER"))
            .organizationCode("ROOT")
            .permissionGroupCode("DEFAULT")
            .build();
    adUser.setActiveDirectoryDomain("corp.example.com");

    UserAccount ssoUser =
        UserAccount.builder()
            .username("sso-user")
            .password(passwordEncoder.encode("unused"))
            .email("sso@example.com")
            .roles(Set.of("ROLE_USER"))
            .organizationCode("ROOT")
            .permissionGroupCode("DEFAULT")
            .build();
    ssoUser.setSsoId("sso-sso-user");

    passwordUser = userAccountService.save(passwordUser);
    passwordHistoryService.record(passwordUser, passwordUser.getPassword());
    userAccountService.save(adUser);
    userAccountService.save(ssoUser);
  }
}
