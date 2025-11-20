package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.PasswordHistory;
import com.example.auth.domain.PasswordHistoryRepository;
import com.example.auth.domain.UserAccount;

@DisplayName("PasswordHistoryService 테스트")
class PasswordHistoryServiceTest {

    @Mock
    private PasswordHistoryRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PolicyToggleProvider policyToggleProvider;

    private PasswordHistoryService service;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AuthPolicyProperties properties = new AuthPolicyProperties();
        properties.setPasswordHistorySize(1);
        org.mockito.BDDMockito.given(policyToggleProvider.isPasswordHistoryEnabled()).willReturn(true);
        service = new PasswordHistoryService(repository, passwordEncoder, properties, policyToggleProvider);
        user = UserAccount.builder()
                .username("tester")
                .password("pw")
                .email("tester@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
    }

    @Test
    @DisplayName("Given 비밀번호 이력 When ensureNotReused 호출 Then 중복 시 예외를 던진다")
    void givenHistoryWhenEnsureThenThrow() {
        PasswordHistory history = new PasswordHistory(user, "encoded");
        given(repository.findByUserOrderByChangedAtDesc(user)).willReturn(List.of(history));
        given(passwordEncoder.matches("raw", "encoded")).willReturn(true);

        assertThatThrownBy(() -> service.ensureNotReused(user, "raw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 새로운 비밀번호 When record 호출 Then 이력 저장 및 초과분 삭제가 이뤄진다")
    void givenPasswordWhenRecordThenPrune() {
        PasswordHistory retained = new PasswordHistory(user, "keep");
        PasswordHistory toDelete = new PasswordHistory(user, "old");
        given(repository.findByUserOrderByChangedAtDesc(user)).willReturn(List.of(retained, toDelete));

        service.record(user, "encoded");

        then(repository).should().save(org.mockito.ArgumentMatchers.any(PasswordHistory.class));
        then(repository).should().delete(toDelete);
    }

    @Test
    @DisplayName("Given 비밀번호 만료 정책 When isExpired 검사 Then 만료 여부를 판별한다")
    void givenExpiryWhenCheckedThenExpire() {
        AuthPolicyProperties properties = new AuthPolicyProperties();
        properties.setPasswordExpiryDays(1);
        PasswordHistoryService historyService = new PasswordHistoryService(repository, passwordEncoder, properties, policyToggleProvider);
        user.updatePassword("hash");
        setPasswordChangedAt(user, Instant.now().minusSeconds(172800));

        assertThat(historyService.isExpired(user)).isTrue();
    }

    private static void setPasswordChangedAt(UserAccount account, Instant instant) {
        try {
            var field = UserAccount.class.getDeclaredField("passwordChangedAt");
            field.setAccessible(true);
            field.set(account, instant);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
