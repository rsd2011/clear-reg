package com.example.server.web;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.policy.RowAccessQuery;
import com.example.common.security.RowScope;
import com.example.dw.application.port.DwOrganizationPort;
import com.example.server.web.dto.DwOrganizationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * DW 조직 조회 API.
 *
 * <p>RowScope는 RowAccessPolicy를 통해 조회합니다.
 */
@RestController
@RequestMapping("/api/dw/organizations")
@Tag(name = "DW Organizations")
public class DwOrganizationController {

    private static final RowScope DEFAULT_ROW_SCOPE = RowScope.ORG;

    private final DwOrganizationPort organizationPort;
    private final RowAccessPolicyProvider rowAccessPolicyProvider;

    public DwOrganizationController(DwOrganizationPort organizationPort,
                                    RowAccessPolicyProvider rowAccessPolicyProvider) {
        this.organizationPort = organizationPort;
        this.rowAccessPolicyProvider = rowAccessPolicyProvider;
    }

    @RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)
    @GetMapping
    @Operation(summary = "List organizations visible to the requester")
    public List<DwOrganizationResponse> organizations() {
        Pageable pageable = Pageable.unpaged();
        AuthContext context = AuthContextHolder.current()
                .orElseThrow(() -> new AccessDeniedException("권한 정보가 없습니다."));
        if (context.organizationCode() == null) {
            throw new AccessDeniedException("조직 스코프 정보가 올바르지 않습니다.");
        }
        RowScope rowScope = resolveRowScope(context);
        var match = com.example.common.policy.MaskingContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return organizationPort.getOrganizations(pageable, rowScope, context.organizationCode())
                .getContent()
                .stream()
                .map(r -> DwOrganizationResponse.fromRecord(r, masker))
                .toList();
    }

    private RowScope resolveRowScope(AuthContext context) {
        RowAccessQuery query = new RowAccessQuery(
                context.feature() != null ? context.feature().name() : null,
                context.action() != null ? context.action().name() : null,
                context.permissionGroupCode(),
                context.orgGroupCodes(),
                null);
        return rowAccessPolicyProvider.evaluate(query)
                .map(RowAccessMatch::getRowScope)
                .orElse(DEFAULT_ROW_SCOPE);
    }
}
