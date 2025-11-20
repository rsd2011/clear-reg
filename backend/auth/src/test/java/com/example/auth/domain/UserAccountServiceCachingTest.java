package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.cache.annotation.EnableCaching;

import com.example.auth.security.PasswordHistoryService;

@SpringJUnitConfig(UserAccountServiceCachingTest.Config.class)
class UserAccountServiceCachingTest {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordHistoryService passwordHistoryService;

    @Test
    void givenCachedUser_whenQueryingTwice_thenRepositoryHitOnce() {
        UserAccount account = UserAccount.builder()
                .username("cache-user")
                .password("secret")
                .roles(Set.of("ROLE_USER"))
                .organizationCode("ORG")
                .permissionGroupCode("DEFAULT")
                .build();
        given(repository.findByUsername("cache-user")).willReturn(Optional.of(account));

        UserAccount first = userAccountService.getByUsernameOrThrow("cache-user");
        UserAccount second = userAccountService.getByUsernameOrThrow("cache-user");

        assertThat(first).isSameAs(second);
        verify(repository, times(1)).findByUsername("cache-user");
    }

    @Test
    void givenCachePopulated_whenPasswordChanges_thenEntryEvicted() {
        UserAccount account = UserAccount.builder()
                .username("evict-user")
                .password("secret")
                .roles(Set.of("ROLE_USER"))
                .organizationCode("ORG")
                .permissionGroupCode("DEFAULT")
                .build();
        given(repository.findByUsername("evict-user")).willReturn(Optional.of(account));
        given(passwordEncoder.encode("Newpassword1!")).willReturn("encoded-password");
        willDoNothing().given(passwordHistoryService).ensureNotReused(account, "Newpassword1!");
        willDoNothing().given(passwordHistoryService).record(account, "encoded-password");

        userAccountService.getByUsernameOrThrow("evict-user"); // populate cache
        userAccountService.changePassword(account, "Newpassword1!"); // evict
        userAccountService.getByUsernameOrThrow("evict-user"); // reload

        verify(repository, times(2)).findByUsername("evict-user");
    }

    @Configuration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("userAccounts");
        }

        @Bean
        UserAccountRepository userAccountRepository() {
            return mock(UserAccountRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }

        @Bean
        PasswordHistoryService passwordHistoryService() {
            return mock(PasswordHistoryService.class);
        }

        @Bean
        UserAccountService userAccountService(UserAccountRepository repository,
                                              PasswordEncoder passwordEncoder,
                                              PasswordHistoryService passwordHistoryService) {
            return new UserAccountService(repository, passwordEncoder, passwordHistoryService);
        }
    }
}
