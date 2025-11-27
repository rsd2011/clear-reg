package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.dwgateway.dw.DwOrganizationPort;

class DwOrganizationControllerAccessTest {

    DwOrganizationPort port = mock(DwOrganizationPort.class);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("rowScope가 없으면 조직 조회를 거부한다")
    void organizations_deniedWhenScopeMissing() {
        AuthContextHolder.set(new AuthContext("user", null, null, null, null, (RowScope) null, java.util.Map.of()));
        DwOrganizationController controller = new DwOrganizationController(port);

        assertThatThrownBy(controller::organizations)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("조직 스코프");
    }
}
