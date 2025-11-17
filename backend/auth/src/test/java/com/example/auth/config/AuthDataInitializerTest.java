package com.example.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.auth.security.PasswordHistoryService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthDataInitializer")
class AuthDataInitializerTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordHistoryService passwordHistoryService;

    private AuthDataInitializer initializer;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(passwordEncoder.encode(any()))
                .thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));
        initializer = new AuthDataInitializer(repository, passwordEncoder, passwordHistoryService);
    }

    @Test
    @DisplayName("Given empty repository When run Then create seed users")
    void givenEmptyRepositoryWhenRunThenSeedUsers() throws Exception {
        given(repository.count()).willReturn(0L);

        initializer.run();

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(3);
    }

    @Test
    @DisplayName("Given existing users When run Then skip seeding")
    void givenExistingUsersWhenRunThenSkip() throws Exception {
        given(repository.count()).willReturn(1L);

        initializer.run();

        verify(repository, never()).save(any());
    }
}
