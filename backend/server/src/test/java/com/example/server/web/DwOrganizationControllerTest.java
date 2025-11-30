package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.security.RowScope;
import com.example.dwgateway.dw.DwOrganizationPort;
import com.example.server.web.dto.DwOrganizationResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwOrganizationController 테스트")
class DwOrganizationControllerTest {

    @Mock
    private DwOrganizationPort organizationPort;

    @Mock
    private RowAccessPolicyProvider rowAccessPolicyProvider;

    private DwOrganizationController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new DwOrganizationController(organizationPort, rowAccessPolicyProvider);
    }

    @Test
    @DisplayName("Given 조직 조회 권한 When 호출하면 Then 조직 목록을 반환한다")
    void givenOrganizations_whenListing_thenReturnList() {
        DwOrganizationPort.DwOrganizationRecord record = new DwOrganizationPort.DwOrganizationRecord(
                UUID.randomUUID(), "ORG001", 1, "Headquarters", null, "ACTIVE",
                LocalDate.of(2020, 1, 1), null, OffsetDateTime.now(ZoneOffset.UTC));

        Pageable pageable = Pageable.unpaged();
        // RowAccessPolicyProvider가 ALL 스코프를 반환하도록 stubbing
        RowAccessMatch match = RowAccessMatch.builder().rowScope(RowScope.ALL).build();
        given(rowAccessPolicyProvider.evaluate(any())).willReturn(Optional.of(match));
        given(organizationPort.getOrganizations(pageable, RowScope.ALL, "ORG001"))
                .willReturn(new PageImpl<>(List.of(record), pageable, 1));

        AuthContextHolder.set(AuthContext.of("tester", "ORG001", "DEFAULT",
                FeatureCode.ORGANIZATION, ActionCode.READ, List.of()));
        List<DwOrganizationResponse> response = controller.organizations();
        AuthContextHolder.clear();

        assertThat(response).hasSize(1);
        verify(organizationPort).getOrganizations(pageable, RowScope.ALL, "ORG001");
    }

    @Test
    @DisplayName("Given 인증 컨텍스트가 없을 때 When 조회하면 Then AccessDeniedException이 발생한다")
    void givenMissingContext_whenListing_thenThrows() {
        AuthContextHolder.clear();

        assertThatThrownBy(controller::organizations)
                .isInstanceOf(AccessDeniedException.class);
    }
}
