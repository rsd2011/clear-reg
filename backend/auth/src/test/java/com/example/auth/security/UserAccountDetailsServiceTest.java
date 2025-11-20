package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountDetailsService 테스트")
class UserAccountDetailsServiceTest {

    @Mock
    private UserAccountRepository repository;

    @Test
    @DisplayName("Given 존재하는 사용자 When loadUserByUsername 호출 Then UserDetails를 반환한다")
    void givenExistingUserWhenLoadThenReturnDetails() {
        var account = UserAccount.builder()
                .username("tester")
                .password("encoded")
                .email("test@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
        given(repository.findByUsername("tester")).willReturn(Optional.of(account));
        UserAccountDetailsService service = new UserAccountDetailsService(repository);

        var userDetails = service.loadUserByUsername("tester");

        assertThat(userDetails.getUsername()).isEqualTo("tester");
        assertThat(userDetails.getAuthorities()).hasSize(1);
    }

    @Test
    @DisplayName("Given 존재하지 않는 사용자 When loadUserByUsername 호출 Then UsernameNotFoundException을 던진다")
    void givenUnknownUserWhenLoadThenThrow() {
        given(repository.findByUsername("missing")).willReturn(Optional.empty());
        UserAccountDetailsService service = new UserAccountDetailsService(repository);

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
