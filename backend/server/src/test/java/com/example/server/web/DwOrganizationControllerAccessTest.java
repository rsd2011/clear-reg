package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.dwgateway.dw.DwOrganizationPort;

import java.util.List;

class DwOrganizationControllerAccessTest {

    DwOrganizationPort port = mock(DwOrganizationPort.class);
    RowAccessPolicyProvider rowAccessPolicyProvider = mock(RowAccessPolicyProvider.class);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("rowScope가 없으면 조직 조회를 거부한다")
    void organizations_deniedWhenScopeMissing() {
        AuthContextHolder.set(AuthContext.of("user", null, null, null, null, List.of()));
        DwOrganizationController controller = new DwOrganizationController(port, rowAccessPolicyProvider);

        assertThatThrownBy(controller::organizations)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("조직 스코프");
    }
}
