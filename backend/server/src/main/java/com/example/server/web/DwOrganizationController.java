package com.example.server.web;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.dwgateway.dw.DwOrganizationPort;
import com.example.server.web.dto.DwOrganizationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dw/organizations")
@Tag(name = "DW Organizations")
public class DwOrganizationController {

    private final DwOrganizationPort organizationPort;

    public DwOrganizationController(DwOrganizationPort organizationPort) {
        this.organizationPort = organizationPort;
    }

    @RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)
    @GetMapping
    @Operation(summary = "List organizations visible to the requester")
    public List<DwOrganizationResponse> organizations() {
        Pageable pageable = Pageable.unpaged();
        AuthContext context = AuthContextHolder.current()
                .orElseThrow(() -> new AccessDeniedException("권한 정보가 없습니다."));
        if (context.rowScope() == null || context.organizationCode() == null) {
            throw new AccessDeniedException("조직 스코프 정보가 올바르지 않습니다.");
        }
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return organizationPort.getOrganizations(pageable, context.rowScope(), context.organizationCode())
                .getContent()
                .stream()
                .map(r -> DwOrganizationResponse.fromRecord(r, masker))
                .toList();
    }
}
