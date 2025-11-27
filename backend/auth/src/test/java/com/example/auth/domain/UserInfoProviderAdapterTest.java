package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.admin.permission.spi.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserInfoProviderAdapter 테스트")
class UserInfoProviderAdapterTest {

    @Mock
    private UserAccountService userAccountService;

    @InjectMocks
    private UserInfoProviderAdapter adapter;

    @Test
    @DisplayName("Given 사용자가 존재할 때 When getByUsernameOrThrow 호출 Then UserAccountService로 위임한다")
    void givenUserExistsWhenGetByUsernameThenDelegate() {
        // given
        String username = "testuser";
        UserAccount expectedUser = UserAccount.builder()
                .username(username)
                .password("encoded")
                .organizationCode("DEFAULT_ORG")
                .permissionGroupCode("DEFAULT_GROUP")
                .build();
        given(userAccountService.getByUsernameOrThrow(username)).willReturn(expectedUser);

        // when
        UserInfo result = adapter.getByUsernameOrThrow(username);

        // then
        assertThat(result).isEqualTo(expectedUser);
        verify(userAccountService).getByUsernameOrThrow(username);
    }

    @Test
    @DisplayName("Given UserAccountService가 UserInfo를 반환할 때 When 호출 Then 그대로 반환한다")
    void givenServiceReturnsUserInfoWhenCallThenReturnSame() {
        // given
        String username = "admin";
        UserAccount userAccount = UserAccount.builder()
                .username(username)
                .password("encoded")
                .organizationCode("ORG001")
                .permissionGroupCode("ADMIN_GROUP")
                .build();
        given(userAccountService.getByUsernameOrThrow(username)).willReturn(userAccount);

        // when
        UserInfo result = adapter.getByUsernameOrThrow(username);

        // then
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getOrganizationCode()).isEqualTo("ORG001");
        assertThat(result.getPermissionGroupCode()).isEqualTo("ADMIN_GROUP");
    }
}
